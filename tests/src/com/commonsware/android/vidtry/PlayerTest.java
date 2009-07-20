package com.commonsware.android.vidtry;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.commonsware.android.vidtry.PlayerTest \
 * com.commonsware.android.vidtry.tests/android.test.InstrumentationTestRunner
 */
public class PlayerTest extends ActivityInstrumentationTestCase<Player> {

    public PlayerTest() {
        super("com.commonsware.android.vidtry", Player.class);
    }

}
