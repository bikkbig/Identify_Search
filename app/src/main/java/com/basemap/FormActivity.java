package com.basemap;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;



import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 005543 on 12/9/2560.
 */

public class FormActivity extends FragmentActivity {

    final String DATE_PATTERN =
            "(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/((19|20)\\d\\d)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form_layout);

        final EditText name = (EditText) findViewById(R.id.editName);
        final EditText email = (EditText) findViewById(R.id.editEmail);
        final EditText age = (EditText) findViewById(R.id.editAge);
        final EditText date = (EditText) findViewById(R.id.editDate);
        final Spinner sex = (Spinner) findViewById(R.id.editSex);
        final Button save = (Button) findViewById(R.id.save);
        final Button cancel = (Button) findViewById(R.id.cancel);
        final String[] item = new String[]{"Select...", "Male", "Female"};
        final TextView vName = (TextView) findViewById(R.id.viewName);
        final TextView vEmail = (TextView) findViewById(R.id.viewEmail);
        final TextView vSex = (TextView) findViewById(R.id.viewSex);
        final TextView vAge = (TextView) findViewById(R.id.viewAge);
        final TextView vDate = (TextView) findViewById(R.id.viewDate);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, item);
        sex.setAdapter(adapter);
        final Calendar calendar = Calendar.getInstance();
        final Calendar myCalendar = Calendar.getInstance();
        final Date today = calendar.getTime();
        final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        final String todayDate = dateFormat.format(today);
        final Pattern datePattern = Pattern.compile("^[0-3][0-9]/[0-1][0-9]/[0-9][0-9][0-9][0-9]");

        final DatePickerDialog.OnDateSetListener pickDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                date.setText(dateFormat.format(myCalendar.getTime()));
//                date.setText(myCalendar.getTime().toString());

            }
        };

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(FormActivity.this, pickDate, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();

            }
        });

//        date.addTextChangedListener(new TextWatcher() {
//            private String current = "";
//            private String ddmmyyyy = "DDMMYYYY";
//            private Calendar cal = Calendar.getInstance();
//
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (!s.toString().equals(current)) {
//                    String clean = s.toString().replaceAll("[^\\d.]", "");
//                    String cleanC = current.replaceAll("[^\\d.]", "");
//
//                    int cl = clean.length();
//                    int sel = cl;
//                    for (int i = 2; i <= cl && i < 6; i += 2) {
//                        sel++;
//                    }
//                    //Fix for pressing delete next to a forward slash
//                    if (clean.equals(cleanC)) sel--;
//
//                    if (clean.length() < 8) {
//                        clean = clean + ddmmyyyy.substring(clean.length());
//                    } else {
//                        //This part makes sure that when we finish entering numbers
//                        //the date is correct, fixing it otherwise
//                        int day = Integer.parseInt(clean.substring(0, 2));
//                        int mon = Integer.parseInt(clean.substring(2, 4));
//                        int year = Integer.parseInt(clean.substring(4, 8));
//
//                        if (mon > 12) mon = 12;
//                        cal.set(Calendar.MONTH, mon - 1);
//                        year = (year < 2017) ? 2017 : (year > 2100) ? 2100 : year;
//                        cal.set(Calendar.YEAR, year);
//                        // ^ first set year for the line below to work correctly
//                        //with leap years - otherwise, date e.g. 29/02/2012
//                        //would be automatically corrected to 28/02/2012
//
//                        day = (day > cal.getActualMaximum(Calendar.DATE)) ? cal.getActualMaximum(Calendar.DATE) : day;
//                        clean = String.format("%02d%02d%02d", day, mon, year);
//                    }
//
//                    clean = String.format("%s/%s/%s", clean.substring(0, 2),
//                            clean.substring(2, 4),
//                            clean.substring(4, 8));
//
//                    sel = sel < 0 ? 0 : sel;
//                    current = clean;
//                    date.setText(current);
//                    date.setSelection(sel < current.length() ? sel : current.length());
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean check = false;
                Date inputDate = null;
                Boolean checkDate = false;
                if (datePattern.matcher(date.getText().toString()).matches()) {
                    try {
                        inputDate = dateFormat.parse(date.getText().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    checkDate = true;
                }

                if (name.getText().toString().length() == 0) {
                    name.setError("Name required!");
                } else if (email.getText().toString().length() == 0) {
                    email.setError("Email required!");
                } else if (!isValidEmail(email.getText().toString())) {
                    email.setError("Not email format!");
                } else if (sex.getSelectedItemPosition() == 0) {
                    vSex.setText("Sex:");
                    ((TextView) sex.getChildAt(0)).setError("Sex required!");
                } else if (age.getText().toString().length() == 0) {
                    age.setError("Age required!");
                } else if (date.getText().toString().length() == 0 || checkDate == false) {
                    date.setError("Date required!");
                } else if (checkDate) {
                    if (!inputDate.after(today)) {
                        date.setError("Date must greater than today!");
                    } else {
                        check = true;
                    }
                }

                if (check) {
                    vName.setText("Name:" + name.getText().toString());
                    vEmail.setText("Email:" + email.getText().toString());
                    vSex.setText("Sex:" + sex.getSelectedItem().toString());
                    vAge.setText("Age:" + age.getText().toString());
                    vDate.setText("Date:" + date.getText().toString());

                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newActivity = new Intent(FormActivity.this, MainActivity.class);
                startActivity(newActivity);
            }
        });

    }


    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


}
