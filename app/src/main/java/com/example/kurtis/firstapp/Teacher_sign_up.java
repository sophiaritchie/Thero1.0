package com.example.kurtis.firstapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class Teacher_sign_up extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    // TODO add teacher/student drop down option. Develop teacher page.

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final String TAG = "Teacher SignUpActivity";
    private FirebaseAuth mAuth;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private TeacherSignUpTask mAuthTask = null;
    private Intent intent;
    // UI references.
    private AutoCompleteTextView mEmailView;
    private View mSignUpFormView;
    private View mProgressView;
    private String errorFocus;
    private EditText mPasswordView;
    private EditText mPasswordVerify;
    private EditText mUsername;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_sign_up);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.teacher_email);
        populateAutoComplete();

        mAuth = FirebaseAuth.getInstance();

        mPasswordView = (EditText) findViewById(R.id.teacher_password);
        mPasswordVerify = (EditText) findViewById(R.id.teacher_password_verify);
        mUsername = (EditText) findViewById(R.id.teacher_username);

        mPasswordVerify.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.teacher_sign_up_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        intent = new Intent(this, teacher_main_menu.class);
// Create an ArrayAdapter using the string array and a default spinner layout
        //  ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
        //        R.array.student_teacher, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        //spinner.setAdapter(adapter);

    }

    @Override
    public void onStart() {
        super.onStart();
    }


    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        /*
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
*/
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String username = mUsername.getText().toString();
        String password2 = mPasswordVerify.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!signInValidFunctions.isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else if (signInValidFunctions.hasIllegalChars(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_entry));
            focusView = mPasswordView;
            cancel = true;
        } else if (!signInValidFunctions.isPasswordsMatch(password, password2)) {
            mPasswordVerify.setError(getString(R.string.error_passwords_not_match));
            focusView = mPasswordVerify;
            cancel = true;
        }

        // Username checks
        if (TextUtils.isEmpty(username)) {
            mUsername.setError(getString(R.string.error_field_required));
            focusView = mUsername;
            cancel = true;
        } else if (!signInValidFunctions.isUsernameValid(username)) {
            mUsername.setError(getString(R.string.error_invalid_username));
            focusView = mUsername;
            cancel = true;
        } else if (signInValidFunctions.hasIllegalChars(username)) {
            mUsername.setError(getString(R.string.error_invalid_entry));
            focusView = mUsername;
            cancel = true;
        }
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!signInValidFunctions.isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new TeacherSignUpTask(email, password, username);
            mAuthTask.execute((Void) null);
        }
    }
// TODO separate these out I think... shouldn't take you too long.


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        mSignUpFormView = findViewById(R.id.teacher_signup_form);
        mProgressView = findViewById(R.id.teacher_progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = 11;

            mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSignUpFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mSignUpFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
      /*  ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);
*/
        //mEmailView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == 16908332) {  // home button id
            Intent intent = new Intent(this, userChoice.class);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class TeacherSignUpTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mUsername;
        private boolean success;
        private String databaseWriteError;
        private String databaseAuthError;

        TeacherSignUpTask(String email, String password, String username) {
            mEmail = email;
            mPassword = password;
            mUsername = username;

        }

        @Override
        protected Boolean doInBackground(Void... params) {


            Executor hi = new Executor() {
                @Override
                public void execute(@NonNull Runnable runnable) {
                    runnable.run();
                }
            };

            //TODO store extra information in database
            Task<AuthResult> task = mAuth.createUserWithEmailAndPassword(mEmail, mPassword)
                    .addOnCompleteListener(hi, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                success = true;

                            } else {
                                // If sign in fails, display a message to the user.
                                Looper.prepare();
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                success = false;

                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthWeakPasswordException e) {
                                    databaseAuthError = getString(R.string.FirebaseWeakPassword);
                                    errorFocus = "mPasswordView";
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    databaseAuthError = getString(R.string.FirebaseNotValid);
                                    errorFocus = "mEmailView";


                                } catch (FirebaseAuthUserCollisionException e) {
                                    databaseAuthError = getString(R.string.FirebaseDoubleEntry);
                                    errorFocus = "mEmailView";
                                } catch (Exception e) {
                                    databaseAuthError = e.getMessage();
                                    errorFocus = "mEmailView";
                                }
                            }

                        }
                    });


            try {
                // Block on a task and get the result synchronously. This is generally done
                // when executing a task inside a separately managed background thread. Doing this
                // on the main (UI) thread can cause your application to become unresponsive.
                Tasks.await(task);
            } catch (ExecutionException e) {
                // The Task failed, this is the same exception you'd get in a non-blocking
                // failure handler.
                // ...
            } catch (InterruptedException e) {
                // An interrupt occurred while waiting for the task to complete.
                // ...
            }

            if (!success) {
                return false;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            String uid = user.getUid();

            DatabaseReference userNameRef = mRootRef.child("teacher_users");  // setting up the user information based on uid
            DatabaseReference uidRef = userNameRef.child(uid);


            uidRef.child("username").setValue(mUsername, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        databaseWriteError = ("Data could not be saved " + databaseError.getMessage());
                        success = false;
                    }
                }
            });


            DatabaseReference usernameUidRef = mRootRef.child("username-uid"); //setting up an index of usernames mapped to uid
            DatabaseReference usernameRef = usernameUidRef.child(mUsername);

            usernameRef.child("uid").setValue(uid, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        databaseWriteError = ("Data could not be saved " + databaseError.getMessage());
                        success = false;
                    }
                }
            });

            DatabaseReference userTypeRef = mRootRef.child("userType"); //setting up an index of usernames mapped to uid
            DatabaseReference teacherStudentRef = userTypeRef.child(uid);

            teacherStudentRef.child("type").setValue("teacher", new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        databaseWriteError = ("Data could not be saved " + databaseError.getMessage());
                        success = false;
                    }
                }
            });

          /*  DatabaseReference teacherListRef = mRootRef.child("teacher-list").child(mUsername); //setting up an index of usernames mapped to uid

            teacherListRef.child(mUsername).setValue("teacher", new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        databaseWriteError = ("Data could not be saved " + databaseError.getMessage());
                        success = false;
                    }
                }
            });*/

            return success;
        }


        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                startActivity(intent);
                finish();
            } else {
                if (databaseAuthError != null) {
                    if (errorFocus == "mPasswordView") {
                        mPasswordView.setError(databaseAuthError);
                        mPasswordView.requestFocus();
                    } else if (errorFocus == "mEmailView") {
                        mEmailView.setError(databaseAuthError);
                        mEmailView.requestFocus();
                    }
                } else {
                    mPasswordVerify.setError(databaseWriteError);
                    mPasswordVerify.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

