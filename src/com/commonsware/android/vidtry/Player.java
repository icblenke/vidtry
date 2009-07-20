/***
	Copyright (c) 2008-2009 CommonsWare, LLC
	
	Licensed under the Apache License, Version 2.0 (the "License"); you may
	not use this file except in compliance with the License. You may obtain
	a copy of the License at
		http://www.apache.org/licenses/LICENSE-2.0
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.commonsware.android.vidtry;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class Player extends Activity implements
        /* OnBufferingUpdateListener, */ OnCompletionListener,
        MediaPlayer.OnPreparedListener, SurfaceHolder.Callback {
	private static String TAG="Vidtry";
	private static String HISTORY_FILE="history.json";
	private int width=0;
	private int height=0;
	private MediaPlayer player;
	private TappableSurfaceView surface;
	private SurfaceHolder holder;
	private View topPanel=null;
	private View bottomPanel=null;
	private long lastActionTime=0L;
	private boolean isPaused=false;
	private Button go=null;
	private AutoCompleteTextView address=null;
	private URLHistory history=null;
	private ProgressBar timeline=null;
	private ImageButton media=null;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		Thread.setDefaultUncaughtExceptionHandler(onBlooey);
		
		setContentView(R.layout.main);
		
		surface=(TappableSurfaceView)findViewById(R.id.surface);
		surface.addTapListener(onTap);
		holder=surface.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		topPanel=findViewById(R.id.top_panel);
		bottomPanel=findViewById(R.id.bottom_panel);
		
		timeline=(ProgressBar)findViewById(R.id.timeline);
		
		media=(ImageButton)findViewById(R.id.media);
		media.setOnClickListener(onMedia);
		
		go=(Button)findViewById(R.id.go);
		go.setOnClickListener(onGo);
		
		address=(AutoCompleteTextView)findViewById(R.id.address);
		address.addTextChangedListener(addressChangeWatcher);
		history=new URLHistory(this,
													 android.R.layout.simple_dropdown_item_1line);
		
		try {
			File historyFile=new File(getFilesDir(), HISTORY_FILE);
			
			if (historyFile.exists()) {
				BufferedReader in=new BufferedReader(new InputStreamReader(openFileInput(HISTORY_FILE)));
				String str;
				StringBuilder buf=new StringBuilder();
				
				while ((str = in.readLine()) != null) {
					buf.append(str);
					buf.append("\n");
				}
				
				in.close();
				history.load(buf.toString());
			}
		}
		catch (Throwable t) {
			Log.e(TAG, "Exception in loading history", t);
			goBlooey(t);
    }
		
		address.setAdapter(history);		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		isPaused=false;
		surface.postDelayed(onEverySecond, 1000);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		isPaused=true;
		
		try {
			OutputStream out=openFileOutput(HISTORY_FILE,
																			MODE_PRIVATE);
			OutputStreamWriter writer=new OutputStreamWriter(out);
			
			history.save(writer);
			writer.close();
		}
		catch (Throwable t) {
			Log.e(TAG, "Exception writing history", t);
			goBlooey(t);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (player!=null) {
			player.release();
			player=null;
		}
		
		surface.removeTapListener(onTap);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		lastActionTime=SystemClock.elapsedRealtime();
		
		if (keyCode==KeyEvent.KEYCODE_BACK &&
				(topPanel.getVisibility()==View.VISIBLE ||
				 bottomPanel.getVisibility()==View.VISIBLE)) {
			clearPanels(true);
		
			return(true);
		}
			
		return(super.onKeyDown(keyCode, event));
	}
	
/*	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
	} */

	public void onCompletion(MediaPlayer arg0) {
		media.setEnabled(false);
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		width=player.getVideoWidth();
		height=player.getVideoHeight();
		
		if (width!=0 && height!=0) {
			holder.setFixedSize(width, height);
			timeline.setProgress(0);
			timeline.setMax(player.getDuration());
			player.start();
		}
			
		media.setEnabled(true);
	}

	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		// no-op
	}

	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
		// no-op
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// no-op
	}

	private void playVideo(String url) {
		try {
			media.setEnabled(false);
			
			if (player==null) {
				player=new MediaPlayer();
				player.setScreenOnWhilePlaying(true);
			}
			else {
				player.stop();
				player.reset();
			}
			
			player.setDataSource(url);
			player.setDisplay(holder);
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setOnPreparedListener(this);
			player.prepareAsync();
//			player.setOnBufferingUpdateListener(this);
			player.setOnCompletionListener(this);
		}
		catch (Throwable t) {
			Log.e(TAG, "Exception in media prep", t);
			goBlooey(t);
		}
	}
	
	private void clearPanels(boolean both) {
		lastActionTime=0;
		
		if (both) {
			topPanel.setVisibility(View.GONE);
		}
		
		bottomPanel.setVisibility(View.GONE);
	}
	
	private void goBlooey(Throwable t) {
		AlertDialog.Builder builder=new AlertDialog.Builder(this);
		
		builder
			.setTitle("Exception!")
			.setMessage(t.toString())
			.setPositiveButton("OK", null)
			.show();
	}
	
	private TappableSurfaceView.TapListener onTap=
		new TappableSurfaceView.TapListener() {
		public void onTap(MotionEvent event) {
			lastActionTime=SystemClock.elapsedRealtime();
			
			if (event.getY()<surface.getHeight()/2) {
				topPanel.setVisibility(View.VISIBLE);
			}
			else {
				bottomPanel.setVisibility(View.VISIBLE);
			}
		}
	};
	
	private Runnable onEverySecond=new Runnable() {
		public void run() {
			if (lastActionTime>0 &&
					SystemClock.elapsedRealtime()-lastActionTime>3000) {
				clearPanels(false);
			}
			
			if (player!=null) {
				timeline.setProgress(player.getCurrentPosition());
			}
			
			if (!isPaused) {
				surface.postDelayed(onEverySecond, 1000);
			}
		}
	};
	
	private View.OnClickListener onGo=new View.OnClickListener() {
		public void onClick(View v) {
			String url=address.getText().toString();
			
			playVideo(url);
			clearPanels(true);
			history.update(url);
		}
	};
	
	private View.OnClickListener onMedia=new View.OnClickListener() {
		public void onClick(View v) {
			lastActionTime=SystemClock.elapsedRealtime();
			
			if (player!=null) {
				if (player.isPlaying()) {
					media.setImageResource(R.drawable.ic_media_play);
					player.pause();
				}
				else {
					media.setImageResource(R.drawable.ic_media_pause);
					player.start();
				}
			}
		}
	};
	
	private TextWatcher addressChangeWatcher=new TextWatcher() {
		public void afterTextChanged(Editable s) {
			lastActionTime=SystemClock.elapsedRealtime();
			go.setEnabled(s.length()>0);
		}
		
		public void beforeTextChanged(CharSequence s, int start,
																	int count, int after) {
			// no-op
		}
		
		public void onTextChanged(CharSequence s, int start,
															int before, int count) {
			// no-op
		}
	};
	
	private Thread.UncaughtExceptionHandler onBlooey=
		new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread thread, Throwable ex) {
			Log.e(TAG, "Uncaught exception", ex);
			goBlooey(ex);
		}
	};
}
