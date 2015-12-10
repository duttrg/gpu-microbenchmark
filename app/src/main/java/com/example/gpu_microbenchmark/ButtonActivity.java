package com.example.gpu_microbenchmark;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ButtonActivity extends Activity {

    int verM_int;
    int app_int;
    int verSh_int;
    int fragSh_int;
    int texM_int;


    Button submitButton;

    RadioGroup verMGroup;
    RadioGroup appGroup;
    RadioGroup verShGroup;
    RadioGroup fragShGroup;
    RadioGroup texMGroup;

    RadioButton verM;
    RadioButton app;
    RadioButton verSh;
    RadioButton fragSh;
    RadioButton texM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button);

        submitButton = (Button)findViewById(R.id.submit);

        //init check.
        verMGroup  = (RadioGroup) findViewById(R.id.verMGroup);
        verM= (RadioButton) findViewById(R.id.verMOption3);
        verMGroup.check(R.id.verMOption3);
        verM_int = Integer.parseInt(verM.getText().toString());

        appGroup  = (RadioGroup) findViewById(R.id.appGroup);
        app= (RadioButton) findViewById(R.id.appOption3);
        appGroup.check(R.id.appOption3);
        app_int = Integer.parseInt(app.getText().toString());

        verShGroup  = (RadioGroup) findViewById(R.id.verShGroup);
        verSh= (RadioButton) findViewById(R.id.verShOption3);
        verShGroup.check(R.id.verShOption3);
        verSh_int = Integer.parseInt(verSh.getText().toString());

        fragShGroup  = (RadioGroup) findViewById(R.id.fragShGroup);
        fragSh= (RadioButton) findViewById(R.id.fragShOption3);
        fragShGroup.check(R.id.fragShOption3);
        fragSh_int = Integer.parseInt(fragSh.getText().toString());

        texMGroup  = (RadioGroup) findViewById(R.id.texMGroup);
        texM= (RadioButton) findViewById(R.id.texMOption3);
        texMGroup.check(R.id.texMOption3);
        texM_int = Integer.parseInt(texM.getText().toString());

        verMGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                verM = (RadioButton) findViewById(verMGroup.getCheckedRadioButtonId());
                verM_int = Integer.parseInt(verM.getText().toString());
            }
        });

        appGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                app = (RadioButton) findViewById(appGroup.getCheckedRadioButtonId());
                app_int = Integer.parseInt(app.getText().toString());
            }
        });

        verShGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                verSh = (RadioButton) findViewById(verShGroup.getCheckedRadioButtonId());
                verSh_int = Integer.parseInt(verSh.getText().toString());
            }
        });

        fragShGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                fragSh = (RadioButton) findViewById(fragShGroup.getCheckedRadioButtonId());
                fragSh_int = Integer.parseInt(fragSh.getText().toString());
            }
        });

        texMGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                texM = (RadioButton) findViewById(texMGroup.getCheckedRadioButtonId());
                texM_int = Integer.parseInt(texM.getText().toString());
            }
        });

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ButtonActivity.this, MainActivity.class);
                intent.putExtra("verM", verM_int);
                intent.putExtra("app", app_int);
                intent.putExtra("verSh", verSh_int);
                intent.putExtra("fragSh", fragSh_int);
                intent.putExtra("texM", texM_int);

                startActivity(intent);
            }
        };

        submitButton.setOnClickListener(listener);
        }
    }