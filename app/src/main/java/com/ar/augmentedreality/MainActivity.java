package com.ar.augmentedreality;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

public class MainActivity extends ListeningActivity   {

    IVoiceControl listener;
    Context context;
    SurfaceView cameraPreview;
    TextView txtResult;
    BarcodeDetector barcodeDetector;
    TextRecognizer textRecognizer;
    CameraSource cameraSource;
    Camera mCamera;
    final int RequestCameraPermissionID = 1001;
    final int RecordAudioPermssionID = 1002;
    private String code;
    private String address = "https://api.outpan.com/v2/products/";
    private String address2 = "?apikey=d6688ab12d7cfae4e114573ff78c44d4";
    private String lahete = "";
    private static final String TAG_NAME = "name";
    private String srcLanguage = "FI";
    private String dstLanguage = "FI";
    private String[] countries = {"Suomi","Englanti", "Ranska","Saksa","Japani","Kiina","Italia","Espanja","Venäjä","Ruotsi"};
    private String[] moodi = {"Teksti","Viivakoodi","Kasvot","Puhe"};
    private Spinner spinner;
    private Spinner spinner2;
    private boolean tekstiMoodi = true;
    private boolean viivaMoodi = false;
    private boolean kasvoMoodi = false;
    private boolean puheMoodi = false;
    private String teksti;
    private String[] kaannokset = { "0","1","2"};
    private int kaannoskerroin = 0;
    private boolean kaannosSaatu = false;
    private int tSuunta = 1;
    private boolean speechS = false;
    private boolean srctyhja = true;
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String KEYPHRASE = "oh mighty computer";
    private static final String MENU_SEARCH = "menu";
    private static MainActivity instance = null;
    private boolean cameraDetection = false;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraPreview.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            case RecordAudioPermssionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioPermssionID);

        cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        txtResult = (TextView) findViewById(R.id.txtResult);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner2 = (Spinner) findViewById(R.id.spinner2);

        context = getApplicationContext();
        VoiceRecognitionListener.getInstance(MainActivity.this).setListener(this);

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build();
        MultiDetector multiDetector = new MultiDetector.Builder().add(barcodeDetector).add(textRecognizer).build();
        cameraSource = new CameraSource.Builder(this, multiDetector).setRequestedPreviewSize(1920, 1080).setAutoFocusEnabled(true).build();

        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, RequestCameraPermissionID);
                    return;
                }
                try {
                    cameraSource.start(cameraPreview.getHolder());
                    //          mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (viivaMoodi) {
                    final SparseArray<Barcode> qrcodes = detections.getDetectedItems();
                    if (qrcodes.size() != 0) {
                        txtResult.post(new Runnable() {
                            @Override
                            public void run() {
                                code = qrcodes.valueAt(0).displayValue;  // Format == 32
                                txtResult.setText("Haetaan " + code);
                                new GetData().execute(address + code + address2);
                            }
                        });
                    }
                }
            }
        });

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                if (tekstiMoodi) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        txtResult.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder value = new StringBuilder();
                                for (int i = 0; i < items.size(); ++i) {
                                    TextBlock item = items.valueAt(i);
                                    value.append(item.getValue());
                                    value.append("\n");
                                }
                                teksti = value.toString();
                                //       txtResult.setText(value.toString());

                                try {
                                    String query = URLEncoder.encode(value.toString(), "UTF-8");
                                    String langpair = URLEncoder.encode(srcLanguage + "|" + dstLanguage, "UTF-8");
                                    String url = "http://mymemory.translated.net/api/get?q=" + query + "&langpair=" + langpair;
                                    System.out.println(url);
                                    new GetTranslate().execute(url);
                                } catch (Exception e) {
                                    System.out.println(e.toString());
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }

        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter2, View v,
                                       int position, long id) {
                if (position == 0) {
                    srcLanguage = "FI";
                    dstLanguage = "EN";
                } else if (position == 1) {
                    srcLanguage = "EN";
                    dstLanguage = "FI";
                } else if (position == 2) {
                    srcLanguage = "FR";
                    dstLanguage = "FI";
                } else if (position == 3) {
                    srcLanguage = "DE";
                    dstLanguage = "FI";
                } else if (position == 4) {
                    srcLanguage = "JP";
                    dstLanguage = "FI";
                } else if (position == 5) {
                    srcLanguage = "CN";
                    dstLanguage = "FI";
                } else if (position == 6) {
                    srcLanguage = "IT";
                    dstLanguage = "FI";
                } else if (position == 7) {
                    srcLanguage = "ES";
                    dstLanguage = "FI";
                } else if (position == 8) {
                    srcLanguage = "RU";
                    dstLanguage = "FI";
                } else if (position == 9) {
                    srcLanguage = "SE";
                    dstLanguage = "FI";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, moodi);
        adapter2.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter2, View v,
                                       int position, long id) {
                if (position == 0) {
                    tekstiMoodi = true;
                    viivaMoodi = false;
                    kasvoMoodi = false;
                    puheMoodi = false;
                    puhe(false);
                    stopFaceDetection();
                } else if (position == 1) {
                    tekstiMoodi = false;
                    viivaMoodi = true;
                    kasvoMoodi = false;
                    puheMoodi = false;
                    puhe(false);
                    stopFaceDetection();
                } else if (position == 2) {
                    tekstiMoodi = false;
                    viivaMoodi = false;
                    kasvoMoodi = true;
                    puheMoodi = false;
                    puhe(false);
                    startFaceDetection();
                } else if (position == 3) {
                    tekstiMoodi = false;
                    viivaMoodi = false;
                    kasvoMoodi = false;
                    puheMoodi = true;
                    puhe(true);
                    stopFaceDetection();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        for (String command : voiceCommands) {
            txtResult.setText(command);
        }
        restartListeningService();
    }


    private class GetData extends AsyncTask<String, Void, String> {

        HashMap<String, String> product;

        @Override
        protected String doInBackground(String... params) {

            URL urlCould;
            HttpsURLConnection connection;
            InputStream in = null;

            try {
                String url = params[0];
                urlCould = new URL(url);
                connection = (HttpsURLConnection) urlCould.openConnection();

                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                connection.connect();

                in = connection.getInputStream();

            } catch (MalformedURLException MEx){

            } catch (IOException IOEx){
                Log.e("Utils", "HTTPS failed to fetch data");
                System.out.println("FFFAF" +IOEx.toString());
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String vastaus = "";

            try {
                String rivi = "";

                while ((rivi = reader.readLine()) != null) {
                    vastaus += rivi;
                }

                product = ParseJSON(vastaus,TAG_NAME);
                if(product != null){
                    vastaus = product.get(TAG_NAME);
                    if(vastaus == null || vastaus == "null"){
                        lahete = "Ei löytynyt...";
                    }
                    else {
                        lahete = "Tuote: " + vastaus;
                    }
                }
                else {
                    lahete = "Ei löytynyt...";
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtResult.setText(lahete);
                    }
                });

            } catch (IOException e) {
                System.out.println("HH" + e.toString());
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println("BB" + e.toString());
                    e.printStackTrace();
                }
            }

            return vastaus;
        }
    }

    private class GetTranslate extends AsyncTask<String, Void, String> {

        HashMap<String, String> product;

        @Override
        protected String doInBackground(String... params) {
            URL urlCould;
            HttpURLConnection connection;
            InputStream in = null;
            String rivi = null;
            String vastaus = null;

            try {
                String url = params[0];
                urlCould = new URL(url);
                connection = (HttpURLConnection) urlCould.openConnection();

                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");
                connection.connect();

                in = connection.getInputStream();

            } catch (MalformedURLException MEx){

            } catch (IOException IOEx){
                Log.e("Utils", "HTTPS failed to fetch data");
                System.out.println("FFFAF" +IOEx.toString());
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            try {
                while ((rivi = reader.readLine()) != null) {
                    vastaus = rivi;
                    System.out.println("Vastaus: " + vastaus);
                }
                product = ParseJSON(vastaus, "translatedText");

                if(tekstiMoodi) {
                    if (product != null) {
                        vastaus = product.get("translatedText");
                        if (vastaus == null || vastaus == "null") {
                        } else {
                            lahete = vastaus;
                            kaannokset[kaannoskerroin] = vastaus;
                            kaannoskerroin += tSuunta;
                            if (kaannoskerroin == 2) {
                                tSuunta = -1;
                            } else if (kaannoskerroin == 0) {
                                tSuunta = 1;
                            }

                            lahete = getCommon(kaannokset);
                            if (lahete != null) {
                                kaannosSaatu = true;
                            }
                        }
                    }
                }

                else if(puheMoodi){
                    if (product != null) {
                        vastaus = product.get("translatedText");
                        lahete = vastaus;
                        kaannosSaatu = true;
                    }
                }

                if(kaannosSaatu){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(dstLanguage == "FI" || puheMoodi) { txtResult.setText(lahete); }
                            else { txtResult.setText(teksti); }
                            kaannosSaatu = false;
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return rivi;
        }
    }

    private String getCommon(String[] taulukko) {
        int count = 1, tempCount = 0;
        String suosittu = taulukko[0];
        String temp;

        for (int i = 0; i < taulukko.length; i++)
        {
            System.out.println(i + " " + taulukko[i]);
        }

        for (int i = 0; i < (taulukko.length - 1); i++)
        {
            temp = taulukko[i];
            tempCount = 0;
            for (int j = 1; j < taulukko.length; j++)
            {
                if (temp.contentEquals(taulukko[j])) {
                    tempCount++;
                }
            }
            if (tempCount > count)
            {
                suosittu = temp;
                count = tempCount;
            }
        }
        if(tempCount < 2){
            return null;
        }
        return suosittu;
    }

    private HashMap<String, String> ParseJSON(String json, String word) {
        HashMap<String, String> product = new HashMap<String, String>();
        String name;
        if (json != null) {
            try {
                JSONObject jsonObj = new JSONObject(json);
                if(word == "translatedText"){
                    name = jsonObj.getJSONObject("responseData").getString(word);
                }
                else {
                    name = jsonObj.getString(word);
                }
                product.put(word, name);

            } catch (JSONException e) {
                e.printStackTrace();
                System.out.println(e.toString());
                return null;
            }
            return product;
        }
        else {
            Log.e("ServiceHandler", "No data received from HTTP request");
            return null;
        }
    }

    private String translate(String text) {
        if(text == null || text == "" || text == "null"){
            return null;
        }
        System.out.println("Sisään menee " + text + " !");

        try {
            String query = URLEncoder.encode(text, "UTF-8");
            String langpair = URLEncoder.encode(srcLanguage + "|" + dstLanguage, "UTF-8");
            String url = "http://mymemory.translated.net/api/get?q=" + query + "&langpair=" + langpair;
            System.out.println(url);
            new GetTranslate().execute(url);
        } catch (Exception e) {
            System.out.println("translate " + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public void text(String input){
        if(input != null && input.length() != 0 && srcLanguage != "FI") { translate(input); }
        else if(input != null && input.length() != 0 && srcLanguage == "FI") {  txtResult.setText(input); }
    }

    private void puhe(boolean status) {
        if(status){
            startListening();
            speechS = true;
        }
        else if(speechS) {
            stopListening();
        }
    }

    protected void initSpeech() {
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Toast.makeText(context, "Speech Recognition is not available",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            sr.setRecognitionListener(VoiceRecognitionListener.getInstance(MainActivity.this));
        }
    }

    protected void startListening() {
        try {
            initSpeech();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 300);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            if (!intent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE))
            {
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                        "com.dummy");
            }
            sr.startListening(intent);
        } catch(Exception ex) {
            System.out.println("Virhe tuli " + ex.toString());
        }
    }

    @Override
    public void restartListeningService() {
        stopListening();
        startListening();
    }

    public void startFaceDetection(){
        Camera.Parameters params = mCamera.getParameters();

        if (params.getMaxNumDetectedFaces() > 0){
            cameraDetection = true;
            mCamera.startFaceDetection();
        }
    }

    public void stopFaceDetection() {
        if(cameraDetection) {
            mCamera.stopFaceDetection();
            cameraDetection = false;
        }
    }

}
