// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.example.graphtutorial;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String SAVED_IS_SIGNED_IN = "isSignedIn";
    private static final String SAVED_USER_NAME = "userName";
    private static final String SAVED_USER_EMAIL = "userEmail";
    private static final String SAVED_USER_TIMEZONE = "userTimeZone";

    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    private View mHeaderView;
    private boolean mIsSignedIn = false;
    private String mUserName = null;
    private String mUserEmail = null;
    private String mUserTimeZone = null;

    private AuthenticationHelper mAuthHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = findViewById(R.id.drawer_layout);

        // Add the hamburger menu icon
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);

        // Set user name and email
        mHeaderView = mNavigationView.getHeaderView(0);
        setSignedInState(mIsSignedIn);

        // Listen for item select events on menu
        mNavigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            // Load the home fragment by default on startup
            openHomeFragment(mUserName);
        } else {
            // Restore state
            mIsSignedIn = savedInstanceState.getBoolean(SAVED_IS_SIGNED_IN);
            mUserName = savedInstanceState.getString(SAVED_USER_NAME);
            mUserEmail = savedInstanceState.getString(SAVED_USER_EMAIL);
            mUserTimeZone = savedInstanceState.getString(SAVED_USER_TIMEZONE);
            setSignedInState(mIsSignedIn);
        }

        // <InitialLoginSnippet>
        showProgressBar();
        // Get the authentication helper
        AuthenticationHelper.getInstance(getApplicationContext())
            .thenAccept(authHelper -> {
                mAuthHelper = authHelper;
                if (!mIsSignedIn) {
                    doSilentSignIn(false);
                } else {
                    hideProgressBar();
                }
            })
            .exceptionally(exception -> {
                Log.e("AUTH", "Error creating auth helper", exception);
                return null;
            });
        // </InitialLoginSnippet>
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_IS_SIGNED_IN, mIsSignedIn);
        outState.putString(SAVED_USER_NAME, mUserName);
        outState.putString(SAVED_USER_EMAIL, mUserEmail);
        outState.putString(SAVED_USER_TIMEZONE, mUserTimeZone);
    }

    // <OnNavItemSelectedSnippet>
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        // Load the fragment that corresponds to the selected item
        switch (menuItem.getItemId()) {
            case R.id.nav_home:
                openHomeFragment(mUserName);
                break;
            case R.id.nav_calendar:
                openCalendarFragment(mUserTimeZone);
                break;
            case R.id.nav_create_event:
                openNewEventFragment(mUserTimeZone);
                break;
            case R.id.nav_signin:
                signIn();
                break;
            case R.id.nav_signout:
                signOut();
                break;
        }

        mDrawer.closeDrawer(GravityCompat.START);

        return true;
    }
    // </OnNavItemSelectedSnippet>

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void showProgressBar()
    {
        FrameLayout container = findViewById(R.id.fragment_container);
        ProgressBar progressBar = findViewById(R.id.progressbar);
        container.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar()
    {
        FrameLayout container = findViewById(R.id.fragment_container);
        ProgressBar progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
    }

    // Update the menu and get the user's name and email
    private void setSignedInState(boolean isSignedIn) {
        mIsSignedIn = isSignedIn;

        mNavigationView.getMenu().clear();
        mNavigationView.inflateMenu(R.menu.drawer_menu);

        Menu menu = mNavigationView.getMenu();

        // Hide/show the Sign in, Calendar, and Sign Out buttons
        if (isSignedIn) {
            menu.removeItem(R.id.nav_signin);
        } else {
            menu.removeItem(R.id.nav_home);
            menu.removeItem(R.id.nav_calendar);
            menu.removeItem(R.id.nav_create_event);
            menu.removeItem(R.id.nav_signout);
        }

        // Set the user name and email in the nav drawer
        TextView userName = mHeaderView.findViewById(R.id.user_name);
        TextView userEmail = mHeaderView.findViewById(R.id.user_email);

        if (isSignedIn) {
            userName.setText(mUserName);
            userEmail.setText(mUserEmail);
        } else {
            mUserName = null;
            mUserEmail = null;
            mUserTimeZone = null;

            userName.setText("Please sign in");
            userEmail.setText("");
        }
    }

    // Load the "Home" fragment
    public void openHomeFragment(String userName) {
        HomeFragment fragment = HomeFragment.createInstance(userName);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
        mNavigationView.setCheckedItem(R.id.nav_home);
    }

    // Load the "Calendar" fragment
    private void openCalendarFragment(String timeZone) {
        CalendarFragment fragment = CalendarFragment.createInstance(timeZone);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
        mNavigationView.setCheckedItem(R.id.nav_calendar);
    }

    // Load the "New Event" fragment
    private void openNewEventFragment(String timeZone) {
        NewEventFragment fragment = NewEventFragment.createInstance(timeZone);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
        mNavigationView.setCheckedItem(R.id.nav_create_event);
    }

    // <SignInAndOutSnippet>
    private void signIn() {
        showProgressBar();
        // Attempt silent sign in first
        // if this fails, the callback will handle doing
        // interactive sign in
        doSilentSignIn(true);
    }

    private void signOut() {
        mAuthHelper.signOut();

        setSignedInState(false);
        openHomeFragment(mUserName);
    }
    // </SignInAndOutSnippet>

    // Silently sign in - used if there is already a
    // user account in the MSAL cache
    private void doSilentSignIn(boolean shouldAttemptInteractive) {
        mAuthHelper.acquireTokenSilently()
            .thenAccept(authenticationResult -> {
                handleSignInSuccess(authenticationResult);
            })
            .exceptionally(exception -> {
                // Check the type of exception and handle appropriately
                Throwable cause = exception.getCause();
                if (cause instanceof MsalUiRequiredException) {
                    Log.d("AUTH", "Interactive login required");
                    if (shouldAttemptInteractive) doInteractiveSignIn();
                } else if (cause instanceof MsalClientException) {
                    MsalClientException clientException = (MsalClientException)cause;
                    if (clientException.getErrorCode() == "no_current_account" ||
                        clientException.getErrorCode() == "no_account_found") {
                        Log.d("AUTH", "No current account, interactive login required");
                        if (shouldAttemptInteractive) doInteractiveSignIn();
                    }
                } else {
                    handleSignInFailure(cause);
                }
                hideProgressBar();
                return null;
            });
    }

    // Prompt the user to sign in
    private void doInteractiveSignIn() {
        mAuthHelper.acquireTokenInteractively(this)
            .thenAccept(authenticationResult -> {
                handleSignInSuccess(authenticationResult);
            })
            .exceptionally(exception -> {
                handleSignInFailure(exception);
                hideProgressBar();
                return null;
            });
    }

    // <HandleSignInSuccessSnippet>
    // Handles the authentication result
    private void handleSignInSuccess(IAuthenticationResult authenticationResult) {
        // Log the token for debug purposes
        String accessToken = authenticationResult.getAccessToken();
        Log.d("AUTH", String.format("Access token: %s", accessToken));

        // Get Graph client and get user
        GraphHelper graphHelper = GraphHelper.getInstance();
        graphHelper.getUser()
            .thenAccept(user -> {
                mUserName = user.displayName;
                mUserEmail = user.mail == null ? user.userPrincipalName : user.mail;
                mUserTimeZone = user.mailboxSettings.timeZone;

                runOnUiThread(() -> {
                    hideProgressBar();
                    setSignedInState(true);
                    openHomeFragment(mUserName);
                });
            })
            .exceptionally(exception -> {
                Log.e("AUTH", "Error getting /me", exception);

                runOnUiThread(()-> {
                    hideProgressBar();
                    setSignedInState(false);
                });

                return null;
            });
    }
    // </HandleSignInSuccessSnippet>

    private void handleSignInFailure(Throwable exception) {
        if (exception instanceof MsalServiceException) {
            // Exception when communicating with the auth server, likely config issue
            Log.e("AUTH", "Service error authenticating", exception);
        } else if (exception instanceof MsalClientException) {
            // Exception inside MSAL, more info inside MsalError.java
            Log.e("AUTH", "Client error authenticating", exception);
        } else {
            Log.e("AUTH", "Unhandled exception authenticating", exception);
        }
    }
}
//TODO: En Uso