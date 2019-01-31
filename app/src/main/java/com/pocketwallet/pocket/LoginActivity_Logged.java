package com.pocketwallet.pocket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class LoginActivity_Logged extends AppCompatActivity {

    final String LOGIN_URL = "http://pocket.ap-southeast-1.elasticbeanstalk.com/users/login";
    final String POSTFCM_URL = "http://pocket.ap-southeast-1.elasticbeanstalk.com/users/fcmtoken";

    private SharedPreferences userPreferences;
    private boolean doubleBackToExitPressedOnce = false;

    private Button fingerprintButton;
    private Button loginButton2;
    private TextView passwordInput;

    String userId;
    String phoneNumber;
    String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_loggedin);

        fingerprintButton = findViewById(R.id.fingerprintButton);
        fingerprintButton.setOnClickListener (new View.OnClickListener() {
            public void onClick(View view){
                RequestFingerprint();
            }
        });

        passwordInput = findViewById(R.id.loginPassword);
        passwordInput.addTextChangedListener(loginTextWatcher);

        loginButton2 = findViewById(R.id.loginButton2);
        loginButton2.setOnClickListener (new View.OnClickListener() {
            public void onClick(View view){
                password = passwordInput.getText().toString();
                login(phoneNumber,"-","0",password);
            }
        });

        //Get user's name from shared preferences
        userPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String user_name = userPreferences.getString("user_name", "Name");
        TextView name = findViewById(R.id.signinName);
        name.setText(user_name);

        KEY_NAME = userPreferences.getString("KEY_NAME", "DEFAULT");
        phoneNumber = userPreferences.getString("PhoneNumber", "DEFAULT");
    }

    //POST LOGIN REQUEST
    private void login(String phoneNumber, String token, String mode,String password) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        try {
            JSONObject jsonBody = new JSONObject();
            if(mode.equals("1")) {
                jsonBody.put("mode", 1);
                jsonBody.put("phoneNumber", phoneNumber);
                jsonBody.put("token", token);
            }else{
                jsonBody.put("mode", 0);
                jsonBody.put("phoneNumber", phoneNumber);
                jsonBody.put("password", password);
            }
            System.out.println("Login Details: " + jsonBody);

            JsonObjectRequest jsonObject = new JsonObjectRequest(Request.Method.POST, LOGIN_URL, jsonBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        System.out.println("Response: " + response);
                        String result = response.getString("result");
                        String userId = response.getString("user_id");
                        System.out.println("Results: " + result);
                        System.out.println("User: " + userId);
                        if(!userId.equals("failed")){
                            postFCMToken(userId);
                            launchMainActivity(userId);
                        }else{
                            System.out.println("===================Failed to Login===================");
                        }

                    }catch(JSONException e){

                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onBackPressed();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    final Map<String, String> headers = new HashMap<>();
                    //headers.put("Authorization", "Basic " + "c2FnYXJAa2FydHBheS5jb206cnMwM2UxQUp5RnQzNkQ5NDBxbjNmUDgzNVE3STAyNzI=");//put your token here
                    return headers;
                }
            };
            requestQueue.add(jsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //POST FCMTOKEN
    private void postFCMToken(String userId) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fcmToken = prefs.getString("FCM_TOKEN", "DEFAULT");
        System.out.println(fcmToken);
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", userId);
            jsonBody.put("fcm_token", fcmToken);
            System.out.println("Login Details: " + jsonBody);
            JsonObjectRequest jsonObject = new JsonObjectRequest(Request.Method.POST, POSTFCM_URL, jsonBody, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String result = response.getString("result");
                        System.out.println("Results: " + result);
                        if (result.equals("success")) {
                            System.out.println("Post FCM Token Success!");
                        } else {
                            System.out.println("Post FCM Token Failed :(");
                        }
                    } catch (JSONException e) {

                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    onBackPressed();
                }
            });
            requestQueue.add(jsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //LAUNCH MAIN ACTIVITY
    public void launchMainActivity(String userId){
        Intent intent = new Intent(LoginActivity_Logged.this, MainActivity.class);
        intent.putExtra("userId",userId);
        startActivity(intent);
        finish();
    }

    //Fingerprint
    String KEY_NAME = "";
    KeyStore keyStore;{
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    };
    Cipher cipher;

    public void RequestFingerprint(){
        generateKey();
        initCipher();

        final FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(this);
        if (!fingerprintManagerCompat.isHardwareDetected()) {
            System.out.println("Device does not have fingerprint scanner");
            return;
        } else if (!fingerprintManagerCompat.hasEnrolledFingerprints()) {
            // User hasn't enrolled any fingerprints to authenticate with
            System.out.println("Devices does not have enrolled fingerprints");
            return;
        }

        Toast toast = Toast.makeText(getApplicationContext(),
                "Please scan your Fingerprint",
                Toast.LENGTH_SHORT);
        toast.show();

        FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
        fingerprintManagerCompat.authenticate(cryptoObject, 0, new CancellationSignal(),
                new FingerprintManagerCompat.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errMsgId, CharSequence errString) {
                        super.onAuthenticationError(errMsgId, errString);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Fingerprint NOT RECOGNIZED",
                                Toast.LENGTH_SHORT);
                        toast.show();
                        //updateStatus(String.valueOf(errString));
                        //biometricCallback.onAuthenticationError(errMsgId, errString);
                    }
                    @Override
                    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                        super.onAuthenticationHelp(helpMsgId, helpString);
                        //updateStatus(String.valueOf(helpString));
                        //
                        Toast toast = Toast.makeText(getApplicationContext(),
                                String.valueOf(helpString),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Authentication Success!",
                                Toast.LENGTH_SHORT);
                        toast.show();
                        login(phoneNumber,KEY_NAME,"1","-");

                        //dismissDialog();
                        //biometricCallback.onAuthenticationSuccessful();
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "Authentication Failed.",
                                Toast.LENGTH_SHORT);
                        toast.show();
                        //updateStatus(context.getString(R.string.biometric_failed));
                        //biometricCallback.onAuthenticationFailed();
                    }
                }, null);
    }

    private void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
        }
    }
    private boolean initCipher() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    private TextWatcher loginTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            password = passwordInput.getText().toString().trim();
            loginButton2.setEnabled(!password.isEmpty());
        }
        @Override
        public void afterTextChanged(Editable s) {

        }
    };
}