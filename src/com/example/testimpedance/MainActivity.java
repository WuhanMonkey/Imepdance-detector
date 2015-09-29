package com.example.testimpedance;




import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Math;



public class MainActivity extends Activity {

	int samplingRate = 44100;
	float mono_frequency = 1000;
	boolean mStop = true;
	boolean recordStop = false;
	MediaPlayer mPlay = null;
	private static final int RECORDER_SAMPLERATE = 44100;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private AudioRecord recorder = null;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	private double max =0;
	private double ohm = 0;
	private double ohm_coord=0;
	private double ohm_offset=0;
	private float stereo_frequency =0;
	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	private Writer writer;
	short sData[] = new short[BufferElements2Rec];
	short sData_lowpass[] = new short[BufferElements2Rec];
	int bufferSize =0;
	float a30, b30, a40, b40, a50, b50; //30k, 40k, 50k coefficient 
	float output30, output40, output50;
	private AudioManager m_amAudioManager;
	private Switch mySwitch;
	static final short ALPHA = (short) 0.25f; // if ALPHA = 1 OR 0, no filter applies.
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
	            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING); 
		mySwitch = (Switch) findViewById(R.id.mySwitch);
		
		  //set the switch to ON 
		  mySwitch.setChecked(true);
		  mySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			    	  if(mySwitch.isChecked()){
						  Log.i("here","nexus");
						  ohm_coord = 2968750;
						  ohm_offset = 2850;
					  }
					  else {
						  Log.i("here", "HTC");
						  ohm_coord = 76819995;
						  ohm_offset = 1969;
					  }   // do something, the isChecked will be
			        // true if the switch is in the On position
			    }
			});
		   
		  //check the current state before we display the screen
		
		 
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
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	//mono sine sensing
/**	public void start(View view) {

	      new Thread( new Runnable( ) 
	      {
	         public void run( )
	         {        		
	            mStop = true;
	            float increment = (float)(2*Math.PI) * mono_frequency / samplingRate; // angular increment for each sample
	            float angle = 0;
	            AndroidAudioDevice device = new AndroidAudioDevice( );
	            float samples[] = new float[1024];
	            
	            
	            runOnUiThread(new Runnable() {
					public void run() {

						Button handle = (Button) findViewById(R.id.inner_start);

						handle.setVisibility(View.INVISIBLE);

						handle = (Button) findViewById(R.id.inner_stop);
						handle.setVisibility(View.VISIBLE);
						
				 		TextView StatusContent = (TextView) findViewById(R.id.status_content);
			    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
			    		StatusContent.setText("Sensing");
			    		StatusDetails
			    				.setText("System is sensing the sensor under mono mode...Press stop button when you want to stop this process.");	 
					}
				});
	            //recording
	            StartRecord();
	            
	            while( mStop )
	            {
	               for( int i = 0; i < samples.length; i++ )
	               {
	                  samples[i] = (float)Math.sin( angle );
	                  angle += increment;
	               }
	 
	               device.writeSamples( samples );
	            }        	
	            stopRecording();            
	            device.release();
	            writeAmplitudeListToFile(sData);

	           
	         }         
	      } ).start();
	}
	
	public void stop(View view){
		mStop = false;
		
		runOnUiThread(new Runnable() {
			public void run() {

				Button handle = (Button) findViewById(R.id.inner_start);

				handle.setVisibility(View.VISIBLE);

				handle = (Button) findViewById(R.id.inner_stop);
				handle.setVisibility(View.INVISIBLE);
				
				TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Ready");
	    		StatusDetails
	    				.setText("Press Start to begin collecting data. Make sure the sensor is connected to the headset interface.");	
			}
		});
	}
	
**/
	public void start_stereo(View view){
		EditText offsetText = (EditText) findViewById(R.id.offset);
		String offset = offsetText.getText().toString();
		final byte offset_lf = Byte.valueOf(offset);
		EditText freqText = (EditText) findViewById(R.id.input_freq);
		String freq = freqText.getText().toString();
		stereo_frequency = Float.valueOf(freq);
		//stereo_frequency = stereo_frequency/2;
		//Log.d("testimpedance", "offset is : " + Byte.toString(offset_lf));
		//Log.d("testimpedance", "freq is : " + Float.toString(stereo_frequency));
		new Thread( new Runnable( ) 
	      {
	         public void run( )
	         {        		
	            mStop = true;
	            float increment = (float)(2*Math.PI) * stereo_frequency / samplingRate; // angular increment for each sample
	            float angle = 0;
	            //AndroidAudioDevice device = new AndroidAudioDevice( );
	            AudioDeviceStereo device = new AudioDeviceStereo();
	            float samples[] = new float[1024];
	            device.changeTheOffset(offset_lf);
	            
	            runOnUiThread(new Runnable() {
					public void run() {

						Button handle = (Button) findViewById(R.id.inner_start_stereo);

						handle.setVisibility(View.INVISIBLE);

						handle = (Button) findViewById(R.id.inner_stop_stereo);
						handle.setVisibility(View.VISIBLE);
						
				 		TextView StatusContent = (TextView) findViewById(R.id.status_content);
			    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
			    		StatusContent.setText("Sensing");
			    		StatusDetails
			    				.setText("System is sensing the sensor under stereo mode...Press stop button when you want to stop this process.");	 
					}
				});
	            StartRecord();
	            while( mStop )
	            {
	               for( int i = 0; i < samples.length; i++ )
	               {
	                  samples[i] = (float)Math.sin( angle );
	                  angle += increment;
	               }
	 
	               device.writeSamples( samples );
	            }        	
	            stopRecording();            
	            device.release();
	            //writeAmplitudeListToFile(sData);
	            //need to make a toast here for the largest amplitude we get.
	            max = 0;
	            //low pass filter
	            sData_lowpass = noisy_filter(sData,sData_lowpass);
	            
	            for(int i =0; i< sData_lowpass.length; i++){
	            	if(max < Math.abs((int)sData_lowpass[i]))
	            		max = Math.abs((int)sData_lowpass[i]);            		
	            }
	            //put the fit calculation here
	            ohm = ohm_coord/max - ohm_offset;
	            //ohm = 2968750/max-2750;
	            //ohm = 78656150/max-1923;
	            runOnUiThread(new Runnable() {
	                public void run() {
	            //Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)ohm, Toast.LENGTH_SHORT).show();
	            Toast.makeText(getApplicationContext(), "Estimated Resistance is "+ (int)ohm + "ohm" + "Estimated ADC is " + (int)max, Toast.LENGTH_SHORT).show();

	                }
	            });
	         }         
	      } ).start();
	
		
	}
	
	protected short[] noisy_filter( short[] input, short[] output ) {
		output[0]=input[0];
		output[1]=input[1];
		output[2]=input[2];

		for(int i = 3; i<input.length-3; i++){
			output[i] = (short) ((input[i]+input[i-1]+input[i-2]+input[i-3]+input[i+1]+input[i+2]+input[i+3])/7);
		}
	    return output;
	}
	
	public void stop_stereo (View view){
		
		mStop = false;
		
		runOnUiThread(new Runnable() {
			public void run() {

				Button handle = (Button) findViewById(R.id.inner_start_stereo);

				handle.setVisibility(View.VISIBLE);

				handle = (Button) findViewById(R.id.inner_stop_stereo);
				handle.setVisibility(View.INVISIBLE);
				
				TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Ready");
	    		StatusDetails
	    				.setText("Press Start to begin collecting data. Make sure the sensor is connected to the headset interface.");	
			}
		});
	}
	
/**	public void Start_wav (View view){
		//TODO Generate wav file programatically, user can adjust the freq and amplitude if needed.
		mPlay = MediaPlayer.create(this, R.raw.stereo);
		 StartRecord();
		mPlay.start();
		runOnUiThread(new Runnable() {
			public void run() {

				Button handle = (Button) findViewById(R.id.inner_start_wav);

				handle.setVisibility(View.INVISIBLE);

				handle = (Button) findViewById(R.id.inner_stop_wav);
				handle.setVisibility(View.VISIBLE);
				
		 		TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Sensing");
	    		StatusDetails
	    				.setText("System is sensing the sensor under wav mode...Press stop button when you want to stop this process.");	 
			}
		});
		
	}
	
	public void Stop_wav (View view){
		mPlay.stop();
		mPlay.release();
		
		runOnUiThread(new Runnable() {
			public void run() {

				Button handle = (Button) findViewById(R.id.inner_start_wav);

				handle.setVisibility(View.VISIBLE);

				handle = (Button) findViewById(R.id.inner_stop_wav);
				handle.setVisibility(View.INVISIBLE);
				
				TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Ready");
	    		StatusDetails
	    				.setText("Press Start to begin collecting data. Make sure the sensor is connected to the headset interface.");	
			}
		});
		
		  stopRecording();            
          writeAmplitudeListToFile(sData);
	}
**/	
	public void StartRecord (){
		
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
	            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
	            RECORDER_AUDIO_ENCODING,  bufferSize);
		
		recorder.startRecording();
	    isRecording = true;
	    recordingThread = new Thread(new Runnable() {
	        public void run() {
	            AudioReading();
	        }
	    }, "AudioRecorder Thread");
	    recordingThread.start();

	}
	
	public void AudioReading(){		
		while (isRecording) {
	        // gets the voice output from microphone to byte format
	        recorder.read(sData, 0, BufferElements2Rec);	        
	    }
		
	}
	
private void writeAmplitudeListToFile(short [] bigbuffer) {
		
		Log.i("here_write", "writing bigbuffer file");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String currentDateandTime = sdf.format(new Date());
		File f = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "AmpList" + currentDateandTime
				+ ".txt");
		
		try{
		writer = new BufferedWriter(new FileWriter(f));
		} catch (IOException e){
			e.printStackTrace();
		}
		for (int i = 0; i< bigbuffer.length; i++) {

			String str = Short.toString(bigbuffer[i]);			
			try {
				writer.write(str);
				writer.write('\n');
			} catch (IOException e) {
				f = null;

				e.printStackTrace();
			}
		}
		Log.i("here_write", "write bigbuffer file done");
		try {
			writer.close();
			f = null;
		} catch (IOException e) {

			f = null;

			e.printStackTrace();

		}

	}

private void stopRecording() {
    // stops the recording activity
    if (null != recorder) {
        isRecording = false;
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;
        }
        
    }

}


