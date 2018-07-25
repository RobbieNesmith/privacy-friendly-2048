/*
 This file is part of Privacy Friendly App Example.

 Privacy Friendly App Example is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Example is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendlyexample.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.secuso.privacyfriendlyexample.R;
import org.secuso.privacyfriendlyexample.activities.helper.BaseActivityWithoutNavBar;
import org.secuso.privacyfriendlyexample.activities.helper.GameState;
import org.secuso.privacyfriendlyexample.activities.helper.GameStatistics;
import org.secuso.privacyfriendlyexample.activities.helper.Gesten;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Christopher Beckmann
 * @version 20161225
 * This activity is an example for the main menu of gaming applications
 */

public class GameActivity extends BaseActivityWithoutNavBar {
    public static int n = 4;
    public TextView textFieldPoints;
    public TextView textFieldRecord;
    public int numberFieldSize = 0;
    static element [][] elements = null;
    static element [][] last_elements = null;
    static element [][] backgroundElements;
    static GameState gameState = null;

    RelativeLayout number_field;
    RelativeLayout number_field_background;
    RelativeLayout touch_field;
    ImageButton restartButton;
    ImageButton undoButton;
    public static int points = 0;
    public static int last_points = 0;
    public static long record = 0;

    public static long ADDINGSPEED = 100;
    public static long MOVINGSPEED = 80;
    public static long SCALINGSPEED = 100;
    public static float SCALINGFACTOR = 1.1f;

    public static boolean moved = false;
    public static boolean firstTime = true;
    public static boolean newGame;
    public boolean won2048=false;
    public static boolean gameOver = false;
    public static boolean createNewGame = true;
    public static boolean undo = false;
    public static boolean animationActivated = true;

    public final int WINTHRESHOLD = 2048;
    public final double PROPABILITYFORTWO = 0.9;
    View.OnTouchListener swipeListener;


    static String filename;

    SharedPreferences sharedPref;

    GameStatistics gameStatistics = new GameStatistics(n);
    public static long startingTime;
    public int highestNumber;


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item= menu.findItem(R.id.action_settings);
        item.setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if(firstTime && intent.getBooleanExtra("new",true)) {
            createNewGame = true;
            firstTime = false;
        }


        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        animationActivated = sharedPref.getBoolean("pref_animationActivated",false);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);




        number_field = (RelativeLayout) findViewById(R.id.number_field);
        number_field_background = (RelativeLayout) findViewById(R.id.number_field_background);
        touch_field = (RelativeLayout) findViewById(R.id.touch_field) ;
        textFieldPoints = (TextView) findViewById(R.id.points);
        textFieldRecord = (TextView) findViewById(R.id.record);
        restartButton = (ImageButton) findViewById(R.id.restartButton);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveStatisticsToFile(gameStatistics);
                createNewGame();
            }
        });
        undoButton = (ImageButton) findViewById(R.id.undoButton);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoButton.setVisibility(View.INVISIBLE);
                if(undo&&last_elements != null) {
                    gameStatistics.undo();
                    elements = last_elements;
                    points = last_points;
                    number_field.removeAllViews();
                    points = last_points;
                    textFieldPoints.setText(""+points);
                    setDPositions(false);
                    for(element[] i : elements)
                    {
                        for(element j: i)
                        {
                            j.setVisibility(View.INVISIBLE);
                            number_field.addView(j);
                            j.drawItem();
                        }
                    }
                    updateGameState();
                    drawAllElements(elements);
                    number_field.refreshDrawableState();
                }
                undo = false;
                Log.i("undoing",display(elements) + " " + number_field.getChildCount());
            }
        });

        //number_field.setBackgroundColor((this.getResources().getColor(R.color.background_gamebord)));
        startingTime = Calendar.getInstance().getTimeInMillis();

    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        createNewGame = false;
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onBackPressed() {
        saveStateToFile(gameState);
        gameStatistics.addTimePlayed(Calendar.getInstance().getTimeInMillis()-startingTime);
        startingTime = Calendar.getInstance().getTimeInMillis();
        saveStatisticsToFile(gameStatistics);
        firstTime = true;

        super.onBackPressed();

    }

    public void createNewGame()
    {
        createNewGame = true;
        getIntent().putExtra("new",true);
        number_field.removeAllViews();
        initialize();

    }

    protected void start()
    {
        Log.d("started","STARTED_________");

        android.view.ViewGroup.LayoutParams lp = number_field.getLayoutParams();

        //setting squared Number Field
        if(number_field.getHeight()>number_field.getWidth())
            lp.height = number_field.getWidth();
        else
            lp.width = number_field.getHeight();
        number_field.setLayoutParams(lp);
        number_field_background.setLayoutParams(lp);

        initialize();
        setListener();
        if(newGame) {
            moved = true;
            addNumber();
        }
        newGame = false;

    }
    public void initializeState()
    {
        points = 0;
        Intent intent = getIntent();
        n = intent.getIntExtra("n", 4);
        newGame = intent.getBooleanExtra("new", true);
        filename = intent.getStringExtra("filename");
        undo = intent.getBooleanExtra("undo",false);
        Log.i("init","undo: "+undo);
        if (!newGame) {
            gameState = readStateFromFile();
            points = gameState.points;
            last_points = gameState.last_points;
        } else {
            gameState = new GameState(n);
            newGame = true;
        }
        elements = new element[n][n];
        last_elements = new element[n][n];
        backgroundElements = new element[n][n];



    }

    public void drawAllElements(element[][] e)
    {
        for(element[] i : e)
        {
            for(element j: i)
            {
                j.drawItem();
            }
        }
    }

    public void updateGameState()
    {
        gameState = new GameState(elements,last_elements);
        gameState.n = n;
        gameState.points = points;
        gameState.last_points = last_points;
        gameState.undo = undo;
        check2048();
        updateHighestNumber();

    }

    public void initialize()
    {
        if(getIntent().getIntExtra("n",4)!=n||createNewGame)
        {
            initializeState();
            Log.i("init","initialializeState()");

        }
        gameStatistics = readStatisticsFromFile();
        record = gameStatistics.getRecord();
        last_points = gameState.last_points;
        createNewGame = false;
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int abstand = (10* metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
        numberFieldSize = number_field.getWidth();
        if(numberFieldSize>number_field.getHeight())
            numberFieldSize = number_field.getHeight();
        int number_size = (numberFieldSize-abstand)/n-abstand;

        textFieldRecord.setText(""+record);
        textFieldPoints.setText(""+points);
        if(undo)
            undoButton.setVisibility(View.VISIBLE);
        else
            undoButton.setVisibility(View.INVISIBLE);

        for(int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                //background elements
                backgroundElements[i][j] = new element(this);
                //backgroundElements[i][j].setVisibility(View.INVISIBLE);

                elements[i][j] = new element(this);
                elements[i][j].setNumber(gameState.getNumber(i,j));
                elements[i][j].drawItem();
                if(elements[i][j].getNumber() >= WINTHRESHOLD)
                    won2048 = true;
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(number_size ,number_size);
                lp.setMarginStart(abstand+j*(number_size+abstand));
                lp.topMargin = abstand+i*(number_size+abstand);
                elements[i][j].setDPosition(lp.getMarginStart(),lp.topMargin);
                elements[i][j].setLayoutParams(lp);
                backgroundElements[i][j].setLayoutParams(lp);
                elements[i][j].updateFontSize();
                backgroundElements[i][j].setLayoutParams(lp);
                number_field_background.addView(backgroundElements[i][j]);
                number_field.addView(elements[i][j]);
            }
        }
        last_elements =deepCopy(elements);
        if(undo)
        {
            for(int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    last_elements[i][j].setNumber(gameState.getLastNumber(i,j));
                }
            }
        }

        if(newGame)
        {
            moved = true;
            addNumber();
            newGame = false;
        }
    }
    public void switchElementPositions(element e1,element e2)
    {
        int i = e1.getdPosX();
        int j = e1.getdPosY();

        e1.animateMoving = true;
        e1.setDPosition(e2.getdPosX(),e2.getdPosY());
        e2.animateMoving = false;
        e2.setDPosition(i,j);

    }

    public element[][] deepCopy(element[][]e)
    {
        element [][] r = new element[e.length][];
        for(int i = 0; i < r.length; i++)
        {
            r[i] = new element [e[i].length];
            for(int j = 0; j < r[i].length; j++)
            {
                r[i][j] = e[i][j].copy();
            }
        }
        return r;
    }

    public void setListener()
    {
        swipeListener = new Gesten(this){
            public boolean onSwipeTop() {
                element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                element s = new element(getApplicationContext());

                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[0][i].number;
                    s.posX = 0;
                    s.posY = i;


                    for(int j = 1; j<elements[i].length;j++)
                    {
                        if(elements[j][i].number != 0 &&( s.number == 0 || s.number == elements[j][i].number))
                        {
                            moved=true;
                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i],elements[s.posX][s.posY]);
                            element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;
                            if(s.number!=0)
                                points += elements[s.posX][s.posY].number;
                            if(s.number !=0)
                                s.posX++;
                            j=s.posX;
                            s.number = elements[j][i].number;

                        }
                        else if(elements[j][i].number != 0)
                        {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[0][i].number;
                    s.posX = 0;
                    s.posY = i;


                    for(int j = 1; j<elements[i].length;j++)
                    {
                        if(elements[j][i].number != 0 && s.number == 0)
                        {
                            moved=true;
                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i],elements[s.posX][s.posY]);
                            element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;
                            if(s.number !=0)
                                s.posX++;
                            j=s.posX;
                            s.number = elements[j][i].number;

                        }
                        else if(s.number != 0)
                        {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                if(moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if(moved)
                    gameStatistics.moveT();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                Log.d("TAG","up");
                //es wurde nach oben gewischt, hier den Code einfügen
                return false;
            }
            public boolean onSwipeRight() {
                element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                element s = new element(getApplicationContext());
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[i][elements[i].length-1].number;
                    s.posX = i;
                    s.posY = elements[i].length-1;


                    for(int j = elements[i].length-2; j >= 0;j--)
                    {
                        if(elements[i][j].number != 0 &&( s.number == 0 || s.number == elements[i][j].number))
                        {
                            moved=true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j],elements[s.posX][s.posY]);
                            element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if(s.number!=0)
                                points += elements[s.posX][s.posY].number;
                            if(s.number !=0)
                                s.posY--;
                            j=s.posY;
                            s.number = elements[i][j].number;
                        }
                        else if(elements[i][j].number != 0)
                        {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[i][elements[i].length-1].number;
                    s.posX = i;
                    s.posY = elements[i].length-1;


                    for(int j = elements[i].length-2; j >= 0;j--)
                    {
                        if(elements[i][j].number != 0 && s.number == 0 )
                        {
                            moved=true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j],elements[s.posX][s.posY]);
                            element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;


                            if(s.number !=0)
                                s.posY--;
                            j=s.posY;
                            s.number = elements[i][j].number;
                        }
                        else if(s.number != 0)
                        {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                if(moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                Log.i("danach",display(last_elements));
                if(moved)
                    gameStatistics.moveR();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                Log.d("TAG","right");

                //es wurde nach rechts gewischt, hier den Code einfügen
                return false;
            }
            public boolean onSwipeLeft() {
                element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                element s = new element(getApplicationContext());
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[i][0].number;
                    s.posX = i;
                    s.posY = 0;


                    for(int j = 1; j<elements[i].length;j++)
                    {
                        if(elements[i][j].number != 0 &&( s.number == 0 || s.number == elements[i][j].number))
                        {
                            moved=true;


                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j],elements[s.posX][s.posY]);
                            element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if(s.number!=0)
                                points += elements[s.posX][s.posY].number;
                            if(s.number !=0)
                                s.posY++;
                            j=s.posY;
                            s.number = elements[i][j].number;
                        }
                        else if(elements[i][j].number != 0)
                        {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[i][0].number;
                    s.posX = i;
                    s.posY = 0;

                    for(int j = 1; j<elements[i].length;j++)
                    {
                        if(elements[i][j].number != 0 && s.number == 0)
                        {
                            moved=true;

                            elements[i][j].setNumber(s.number + elements[i][j].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[i][j],elements[s.posX][s.posY]);
                            element z = elements[i][j];
                            elements[i][j] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if(s.number !=0)
                                s.posY++;
                            j=s.posY;
                            s.number = elements[i][j].number;
                        }
                        else if(s.number != 0)
                        {
                            s.number = elements[i][j].number;
                            s.posX = i;
                            s.posY = j;
                        }
                    }

                }
                if(moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                Log.i("danach",display(last_elements));
                if(moved)
                    gameStatistics.moveL();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                Log.d("TAG","left");
                //es wurde nach links gewischt, hier den Code einfügen
                return false;
            }
            public boolean onSwipeBottom() {
                element[][] temp = deepCopy(elements);
                int temp_points = points;
                moved = false;
                element s = new element(getApplicationContext());
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[elements[i].length-1][i].number;
                    s.posX = elements[i].length-1;
                    s.posY = i;


                    for(int j = elements[i].length-2; j>=0;j--)
                    {
                        if(elements[j][i].number != 0 &&( s.number == 0 || s.number == elements[j][i].number))
                        {
                            moved=true;

                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i],elements[s.posX][s.posY]);
                            element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if(s.number!=0)
                                points += elements[s.posX][s.posY].number;
                            if(s.number !=0)
                                s.posX--;
                            j=s.posX;
                            s.number = elements[j][i].number;
                        }
                        else if(elements[j][i].number != 0)
                        {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                for(int i = 0; i < elements.length;i++)
                {
                    s.number =  elements[elements[i].length-1][i].number;
                    s.posX = elements[i].length-1;
                    s.posY = i;


                    for(int j = elements[i].length-2; j>=0;j--)
                    {
                        if(elements[j][i].number != 0 &&s.number == 0)
                        {
                            moved=true;

                            elements[j][i].setNumber(s.number + elements[j][i].number);
                            elements[s.posX][s.posY].setNumber(0);
                            switchElementPositions(elements[j][i],elements[s.posX][s.posY]);
                            element z = elements[j][i];
                            elements[j][i] = elements[s.posX][s.posY];
                            elements[s.posX][s.posY] = z;

                            if(s.number !=0)
                                s.posX--;
                            j=s.posX;
                            s.number = elements[j][i].number;
                        }
                        else if(s.number != 0)
                        {
                            s.number = elements[j][i].number;
                            s.posX = j;
                            s.posY = i;
                        }
                    }

                }
                if(moved) {
                    gameStatistics.addMoves(1);
                    last_points = temp_points;
                    last_elements = temp;
                    undoButton.setVisibility(View.VISIBLE);
                    undo = true;
                }
                if(moved)
                    gameStatistics.moveD();
                addNumber();
                setDPositions(animationActivated);
                updateGameState();
                Log.d("TAG","down");
                //es wurde nach unten gewischt, hier den Code einfügen
                return false;
            }
            public boolean nichts(){
                Log.d("TAG","nothing");
                //es wurde keine wischrichtung erkannt, hier den Code einfügen
                return false;
            }
        };
        touch_field.setOnTouchListener(swipeListener);
        number_field.setOnTouchListener(swipeListener);
        for(int i = 0; i < elements.length; i++) {
            for (int j = 0; j < elements[i].length; j++) {
                elements[i][j].setOnTouchListener(swipeListener);
                backgroundElements[i][j].setOnTouchListener(swipeListener);
            }
        }
    }
    public String display(element[][] e)
    {
        String result = "\n";
        for(int i = 0; i < e.length; i++)
        {
            for(int j = 0; j < e[i].length;j++)
                result = result + " " + elements[i][j].number; //+ " "+elements[i][j];
            result = result + "\n";
        }
        result += "\n";
        for(int i = 0; i < e.length; i++)
        {
            for(int j = 0; j < e[i].length;j++)
                result = result + " (" + elements[i][j].getX() + " , " + elements[i][j].getY() + ")" + " v:" + elements[i][j].getVisibility();//+" "+elements[i][j];
            result = result + "\n";
        }
        return result;
    }

    public void updateHighestNumber()
    {
        for(int i = 0; i < elements.length;i++)
        {
            for(int j = 0; j < elements[i].length;j++)
            {
                if(highestNumber < elements[i][j].number)
                {
                    highestNumber = elements[i][j].number;
                    gameStatistics.setHighestNumber(highestNumber);
                }
            }
        }
    }

    public void check2048()
    {
        if(won2048 == false)
        for(int i = 0; i < elements.length;i++)
        {
            for(int j = 0; j < elements[i].length;j++)
            {
                if(elements[i][j].number==WINTHRESHOLD)
                {
                    Log.i("INFO","" + WINTHRESHOLD + " erreicht");
                    //MESSAGE
                    new AlertDialog.Builder(this)
                            .setTitle((this.getResources().getString(R.string.Titel_V_Message)))
                            .setMessage((this.getResources().getString(R.string.Winning_Message)))
                            .setNegativeButton((this.getResources().getString(R.string.No_Message)), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("Ende","nein");
                                    onBackPressed();

                                }
                            })
                            .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i("Ende","ja");
                                }
                            })
                            .setCancelable(false)
                            .create().show();
                    won2048=true;
                }
            }
        }
    }
    public void setDPositions(boolean animation)
    {
        long SCALINGSPEED = GameActivity.SCALINGSPEED;
        long ADDINGSPEED = GameActivity.ADDINGSPEED;
        long MOVINGSPEED = GameActivity.MOVINGSPEED;
        boolean scale = true;
        if(!animation)
        {
            SCALINGSPEED = 1;
            ADDINGSPEED = 1;
            MOVINGSPEED = 1;
            scale = false;
        }
        for(element[] i: elements)
        {
            for(element j:i)
            {
                if(j.dPosX != j.getX())
                {
                    if(j.animateMoving&&animation)
                    {
                        if(j.number != j.dNumber)
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,scale)).start();
                        else
                            j.animate().x(j.dPosX).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,false)).start();

                    }
                    else {
                        if(!animation) {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.leftMargin = j.dPosX;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        }
                        else
                            j.animate().x(j.dPosX).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,false)).start();

                    }

                }
                if(j.dPosY != j.getY())
                {
                    if(j.animateMoving&&animation)
                    {
                        if(j.number != j.dNumber)
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,scale)).start();
                        else
                            j.animate().y(j.dPosY).setDuration(MOVINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,false)).start();

                    }
                    else {
                        if(!animation)
                        {
                            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) j.getLayoutParams();
                            lp1.topMargin = j.dPosY;
                            j.setLayoutParams(lp1);
                            j.drawItem();
                        }
                        else
                            j.animate().y(j.dPosY).setDuration(0).setStartDelay(MOVINGSPEED).setInterpolator(new LinearInterpolator()).setListener(new MovingListener(j,false)).start();

                    }


                }
            }
        }
    }

    class MovingListener extends AnimatorListenerAdapter {
        element e = null;
        long SCALINGSPEED = 100;
        float scalingFactor = 1.5f;
        boolean scale =false;
        public MovingListener(element e, boolean scale )
        {
            super();
            this.e = e;
            this.SCALINGSPEED = GameActivity.SCALINGSPEED;
            this.scalingFactor = GameActivity.SCALINGFACTOR;
            this.scale = scale;
        }
        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            animation.setupEndValues();
            if(e!=null)
                e.drawItem();
        }
        @Override
        public void onAnimationPause(Animator animation){
            super.onAnimationPause(animation);
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if(e!=null) {
                e.drawItem();
                if(scale)
                    e.animate().scaleX(scalingFactor).scaleY(scalingFactor).setDuration(SCALINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new ScalingListener(e)).start();
            }

        }
    }

    class ScalingListener extends AnimatorListenerAdapter {
        element e = null;
        public ScalingListener(element e)
        {
            super();
            this.e = e;
        }
        public ScalingListener()
        {
            super();
        }
        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            animation.setupEndValues();
        }
        @Override
        public void onAnimationPause(Animator animation){
            super.onAnimationPause(animation);
        }
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if(e!=null) {
                e.animate().scaleX(1.0f).scaleY(1.0f).setDuration(SCALINGSPEED).setStartDelay(0).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                    }
                }).start();
            }

        }
    }

    public void addNumber()
    {

        if(points>record) {
            record = points;
            gameStatistics.setRecord(record);
            textFieldRecord.setText(""+record);
        }
        if(moved) {
            moved = false;
            textFieldPoints.setText("" + points);
            element[] empty_fields = new element[n * n];
            int counter = 0;
            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < elements[i].length; j++) {
                    if (elements[i][j].number == 0) {
                        empty_fields[counter++] = elements[i][j];
                    }
                }
            }
            if(counter>0) {
                int index = (int) (Math.random() * counter);
                int number = 2;
                if (Math.random() > PROPABILITYFORTWO)
                    number = 4;

                empty_fields[index].setNumber(number);
                empty_fields[index].drawItem();
                if(animationActivated){
                    empty_fields[index].setAlpha(0);
                    empty_fields[index].animate().alpha(1).setInterpolator(new LinearInterpolator()).setStartDelay(MOVINGSPEED).setDuration(ADDINGSPEED).start();
                }
                if(counter == 1)
                {
                    gameOver = true;
                    for (int i = 0; i < elements.length; i++) {
                        for (int j = 0; j < elements[i].length; j++) {
                            if ((i+1 < elements.length && elements[i][j].number == elements[i+1][j].number)|| (j+1 < elements[i].length && elements[i][j].number == elements[i][j+1].number)) {
                                gameOver = false;
                                break;
                            }
                        }
                    }
                    if(gameOver)
                    {
                        gameOver();
                    }
                }
            }
            updateGameState();
        }
    }
    public void gameOver()
    {
        new AlertDialog.Builder(this)
                .setTitle((this.getResources().getString(R.string.Titel_L_Message)))
                .setMessage((this.getResources().getString(R.string.Lost_Message)))
                .setNegativeButton((this.getResources().getString(R.string.No_Message)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("Ende","nein");
                        Log.i("StateFile ", "deleted: " + deleteStateFile(filename));
                        //saveRecordToFile(record);
                        saveStatisticsToFile(gameStatistics);
                        createNewGame = true;
                        getIntent().putExtra("new",true);
                        initialize();
                        GameActivity.this.onBackPressed();

                    }
                })
                .setPositiveButton((this.getResources().getString(R.string.Yes_Message)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("Ende","ja");
                        saveStatisticsToFile(gameStatistics);
                        createNewGame();
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
        else if( id == android.R.id.home){
            saveStateToFile(gameState);
            gameStatistics.addTimePlayed(Calendar.getInstance().getTimeInMillis()-startingTime);
            startingTime = Calendar.getInstance().getTimeInMillis();
            saveStatisticsToFile(gameStatistics);
            firstTime = true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        start();


    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        gameStatistics.addTimePlayed(Calendar.getInstance().getTimeInMillis()-startingTime);
        startingTime = Calendar.getInstance().getTimeInMillis();
        saveStateToFile(gameState);
        saveStatisticsToFile(gameStatistics);

    }

    public void saveStateToFile(GameState nS)
    {
        Log.i("saving", ""+nS);
        try {
            if(filename == null)
                filename = "state" + n + ".txt";
            File file = new File(getFilesDir(), filename);
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(nS);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteStateFile(String filename)
    {
        try{
            File directory = getFilesDir();
            File f = new File(directory,filename);
            return f.delete();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    public GameState readStateFromFile()
    {
        GameState nS = new GameState(n);
        try{
            File file = new File(getFilesDir(), filename);
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            nS = (GameState)in.readObject();
            boolean emptyField = true;
            for(int i = 0; i <nS.numbers.length;i++)
            {
                if(nS.numbers[i]>0)
                {
                    emptyField = false;
                    break;
                }
            }
            if(emptyField||nS.n != n) {
                nS = new GameState(n);
                newGame = true;
            }
            in.close();
            fileIn.close();
        }
        catch(Exception e)
        {
            newGame = true;
            e.printStackTrace();
        }
        return nS;
    }
    public GameStatistics readStatisticsFromFile()
    {
        GameStatistics gS = new GameStatistics(n);
        try{
            File file = new File(getFilesDir(), "statistics" + n + ".txt");
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            gS = (GameStatistics)in.readObject();
            in.close();
            fileIn.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return gS;
    }
    public void saveStatisticsToFile(GameStatistics gS)
    {
        try {
            File file = new File(getFilesDir(), gS.getFilename());
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(gS);
            out.close();
            fileOut.close();
            Log.i("saving", "Game Statistics "+ n);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
