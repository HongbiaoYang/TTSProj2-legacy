package com.utkise.TTSProj2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.util.*;

public class activity_vision extends Activity {
    private ImageView  ok;
    private TextView  vText;
    private int mIsDown;
    private float mPrevX, mPrevY;
    private DIRECTION direction;
    private LinearLayout screen;
    private float oldDist;
    private int onHold, onThird;
    private int mTouchCount = 0;
    private List<ItemStruct> root;
    private ItemStruct curItem;
    private int curIndex, firstIndex;
    private List<ItemStruct> curLevel;
    private Stack<List<ItemStruct>> itemStack;
    private Tutorial tutorial;
    private SharedPreferences pref;


    @Override
    public void onBackPressed() {
        if (itemStack.isEmpty()) {
            MyProperties.getInstance().shutup();
            MyProperties.getInstance().popStacks();
            finish();
        } else  {
            detectUP();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_vision);

        screen = (LinearLayout)findViewById(R.id.visionScreen);
        ok    = (ImageView)findViewById(R.id.Ok);
        vText = (TextView)findViewById(R.id.visionText);

        root = new ArrayList<ItemStruct>();
        fillEmergencyList(root, MyProperties.getInstance().emergency);
        fillItemList(root, MyProperties.getInstance().boarding);
        fillItemList(root, MyProperties.getInstance().traveling);
        fillItemList(root, MyProperties.getInstance().gettingoff);

        // first animation in vision
        ImageView image = (ImageView) findViewById(R.id.frame_home);
        image.setBackgroundResource(R.drawable.frame);
        MyProperties.getInstance().animStack.push((AnimationDrawable) image.getBackground());

        // first display
        curIndex = 0;
        firstIndex = 0;
        curItem = root.get(curIndex);
        curLevel = root;
        itemStack = new Stack<List<ItemStruct>>();

        displayCurrent(curItem);
        MyProperties.getInstance().speakout(curItem.getText());

        screen.setOnTouchListener(new visionTouchListener());
        direction = DIRECTION.EMPTY;

        pref = this.getSharedPreferences("com.utkise.TTSProj2", Context.MODE_PRIVATE);
        boolean done = pref.getBoolean("tutorial_vision", false);

        if (done == false)  {
            tutorial = new Tutorial();
            tutorial.startTutorial();
        } else {
            tutorial = null;
        }
    }

    private void fillEmergencyList(List<ItemStruct> node, DisableType emergency) {

        /*// create image and text of this item
        level.setImageID(emergency.getImage());
        level.setVImageID(emergency.getImageV());
        level.setText(LANG.ENGLISH, emergency.getTag());*/

        // next level item
        ItemStruct eItem;
        eItem = new ItemStruct(R.drawable.emergency_v, "Emergency");

        // next level list
        List<ItemStruct> eInfo;
        eInfo = emergency.getEmergency();

        // set children
        eItem.setChild(eInfo);

        node.add(eItem);
    }


    private void fillItemList(List<ItemStruct> node, DisableType type) {
        ItemStruct level = new ItemStruct();

        // create image and text of this item
        level.setImageID(type.getImage());
        level.setVImageID(type.getImageV());
        level.setText(LANG.ENGLISH, type.getTag());

        // next level item
        ItemStruct gItem, tItem, sItem, cItem;
        gItem = new ItemStruct(R.drawable.generalinfo_v, "General Information");
        tItem = new ItemStruct(R.drawable.tripinfo_v, "Trip Information");
        sItem = new ItemStruct(R.drawable.safety_v, "Safety");
        cItem = new ItemStruct(R.drawable.comfort_v, "Comfort");

        // next level list
        List<ItemStruct> gInfo, tInfo, sInfo, cInfo;
        gInfo = type.getGeneralInfo();
        tInfo = type.getTripInfo();
        sInfo = type.getSafety();
        cInfo = type.getComfort();

        // set children
        gItem.setChild(gInfo);
        tItem.setChild(tInfo);
        sItem.setChild(sInfo);
        cItem.setChild(cInfo);

        // add item (general info, trip info, safety, comfort)
        List<ItemStruct> infoList = new ArrayList<ItemStruct>();
        infoList.add(gItem);
        infoList.add(tItem);
        infoList.add(sItem);
        infoList.add(cItem);

        // set top children (boarding, )
        level.setChild(infoList);

        node.add(level);
    }

    private void captureSwipe(float dx, float dy) {

        Log.i("gesture", "dx="+dx + " dy="+dy);
        if (Math.abs(dx)+Math.abs(dy) < 1) {
            direction = DIRECTION.EMPTY;
            return;
        }

        if (Math.abs(dy) > Math.abs(dx)) {
            if (dy > 0) {
                direction = DIRECTION.DOWN;
            }  else {
                direction = DIRECTION.UP;
            }
        }  else {
            if (dx > 0) {
                direction = DIRECTION.RIGHT;
            }  else {
                direction = DIRECTION.LEFT;
            }

        }


    }


    private class visionTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            Handler handler = new Handler();
            Runnable runHold = new Runnable() {
                @Override
                public void run() {
                    Log.i("activity_vision", "Run.onhold="+onHold);
                    if (onHold == 2) {
                        onHold = 0;

                        detectLongPress2();
                        if (tutorial != null) {
                            tutorial.checkNext(DIRECTION.HOLD_FINGER2);
                        }
                    } else if (onHold == 3) {
                        onHold = 0;

                        detectLongPress3();
                        if (tutorial!= null && tutorial.checkNext(DIRECTION.HOLD_FINGER3)) {
                            pref.edit().putBoolean("tutorial_vision", true).apply();
                        }

                    }

                }
            };
            Runnable runTap = new Runnable() {
                @Override
                public void run() {
                    if (mTouchCount == 2) {

                        detectDoubleClick();
                        if (tutorial!= null) {
                            tutorial.checkNext(DIRECTION.DOUBLE_CLICK);
                        }
                    } else if (mTouchCount == 3) {

                        detectTripleClick();
                        if (tutorial!= null) {
                            tutorial.checkNext(DIRECTION.TRIPLE_CLICK);
                        }
                    } else if (mTouchCount == 4) {

                        detectFourClick();

                    }

                    mTouchCount = 0;
                }
            };

            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsDown = 1;
                    mTouchCount ++;

                    onHold = 0;

                    Log.i("gesture","holdtime="+onHold);

                    // detect multiple tap
                    if (mTouchCount == 1) {
                        handler.postDelayed(runTap, 1000);
                    }

                    break;
                case MotionEvent.ACTION_MOVE:

                    if (mIsDown == 1) {

                        float dx = x - mPrevX;
                        float dy = y - mPrevY;

                        if (dx > 10 || dy > 10) {
                            onHold = 0;
                        }

                        captureSwipe(dx, dy);
                    } else{
                        float newDist = spacing(event);
                        if (newDist > oldDist) {
                            Log.i("gesture","zoom in");
                            // MyProperties.getInstance().speakout("Enter");
                        }  else if (newDist < oldDist) {
                            Log.i("gesture","zoom out");
                            // MyProperties.getInstance().speakout("Leave");
                        }

                        Log.i("gesture", "dist=" + newDist);

                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("gesture", "released");
                    Log.i("gesture", "direction="+direction);
                    onHold = 0;

                    if (direction != DIRECTION.EMPTY) {
                        // MyProperties.getInstance().speakout(direction);
                        detectSwipe(direction);

                        mIsDown = 0;
                        direction = DIRECTION.EMPTY;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    onHold = 0;
                    mIsDown -= 1;
                    break;
                case MotionEvent.ACTION_POINTER_2_DOWN:
                    onHold = 2;
                    handler.postDelayed(runHold, 2000);
                    Log.i("gesture","second touched detected");
                    mIsDown += 1;
                    oldDist = spacing(event);
                    break;
                case MotionEvent.ACTION_POINTER_2_UP:
                    onHold = 0;
                    mIsDown -=1;
                    break;
                case MotionEvent.ACTION_POINTER_3_DOWN:
                    onHold = 3;
                    mIsDown +=1;
                    Log.i("gesture", "third finger detected");
                    break;
                case MotionEvent.ACTION_POINTER_3_UP:
                    onHold = 0;
                    mIsDown -=1;
                    break;
            }

            mPrevX = x;
            mPrevY = y;


            return true;
        }


    }

    private void detectLongPress3() {
        MyProperties.getInstance().speakout("more");
        displayResponsePage();
    }

    // perform action based on directions
    private void detectSwipe(DIRECTION dir) {
        Log.i("activity_vision","in:detectSwipe="+dir);

        switch (dir) {
            case LEFT:
                detectLeft();
                break;
            case RIGHT:
                detectRight();
                break;
            case UP:
                Log.i("activity_vision","in:detectSwipe:Up case");
                detectUP();
                break;
            case DOWN:
                detectDown();
                break;
            case EMPTY:
                break;
            default:
                Log.i("activity_vision","in:detectSwipe:default case"+dir);
                break;
        }


        if (tutorial != null && dir != DIRECTION.EMPTY) {
            tutorial.checkNext(dir);
        }


    }

    private void displayCurrent(ItemStruct item) {

        if (item.getVImageID() == 0) {
            ok.setBackgroundResource(item.getImageID());
        } else {
            ok.setBackgroundResource(item.getVImageID());
        }
        vText.setText(item.getText());

    }

    // swipe down, enter next level
    private void detectDown() {
        if (curItem.getChild() == null) {
            String hint;
            hint = getResources().getString(R.string.nodown);

            MyProperties.getInstance().speakout(hint);
        } else {
            itemStack.push(curLevel);
            curLevel = curItem.getChild();
            curIndex = 0;
            curItem = curLevel.get(curIndex);
            displayCurrent(curItem);
            
            MyProperties.getInstance().speakout(curItem.getText());
        }

    }

    // swipe up, go to higher level
    private void detectUP() {
        if (itemStack.isEmpty()) {
            String hint;
            hint = getResources().getString(R.string.noup);

            MyProperties.getInstance().speakout(hint);
        } else {
            curLevel = itemStack.pop();
            curIndex = 0;
            // when go to higher level, restore firstIndex to 0. In response page this firstIndex might be changed
            firstIndex = 0;
            curItem = curLevel.get(curIndex);
            displayCurrent(curItem);

            Log.i("activity_vision","swipe up detected");
            
            MyProperties.getInstance().speakout(curItem.getText());
        }


    }

    // swipe right, next item
    private void detectRight() {

        if (curIndex <= firstIndex) {
            String hint;
            if (curItem.getChild() == null) {
                hint = getResources().getString(R.string.norightq);
            } else {
                hint = getResources().getString(R.string.norightc);
            }

            MyProperties.getInstance().speakout(hint);
        }  else {
            curIndex--;
            curItem = curLevel.get(curIndex);
            displayCurrent(curItem);

            MyProperties.getInstance().speakout(curItem.getText());
        }

    }

    // swipe left, last item
    private void detectLeft() {

        if (curIndex >= curLevel.size() - 1) {
            String hint;
            if (curItem.getChild() == null) {
                hint = getResources().getString(R.string.noleftq);
            } else {
                hint = getResources().getString(R.string.noleftc);
            }

            MyProperties.getInstance().speakout(hint);
        }  else {
            curIndex++;
            curItem = curLevel.get(curIndex);
            displayCurrent(curItem);

            MyProperties.getInstance().speakout(curItem.getText());
        }

    }

    // long press the screen
    private void detectLongPress2() {


        MyProperties.getInstance().shutup();
        MyProperties.getInstance().popStacks();
        finish();

        /*curLevel = root;
        curIndex = 0;
        curItem = curLevel.get(curIndex);
        itemStack.clear();

        displayCurrent(curItem);
        MyProperties.getInstance().speakout(curItem.getText());*/

    }

    // double click, say yes
    private void detectDoubleClick() {
        MyProperties.getInstance().speakout("Yes");
    }

    // triple click, say no
    private void detectTripleClick() {
        MyProperties.getInstance().speakout("No");
    }

    // four click, go to  more
    private void detectFourClick() {
        MyProperties.getInstance().speakout("four taps");

    }

    private void displayResponsePage() {
        itemStack.push(curLevel);
        curLevel = MyProperties.getInstance().response.getInformation("response");

        // get the first item place, ignore the previous items added by the user
        firstIndex = MyProperties.getInstance().response.getCustomCount();
        curIndex = firstIndex;

        curItem = curLevel.get(curIndex);
        displayCurrent(curItem);

        MyProperties.getInstance().speakout(curItem.getText());
    }


    private float spacing(MotionEvent event) {
        if (event.getPointerCount() <= 1) {
            return 0.0f;
        }

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

}
