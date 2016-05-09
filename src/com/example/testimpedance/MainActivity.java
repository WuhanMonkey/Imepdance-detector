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
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.opencsv.CSVWriter;



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
	private double left_max =0;
	private double right_max =0;
	private double ohm = 0;
	private double ohm_coord=0;
	private double ohm_offset=0;
	private double Rint =0;
	private double K=0;
	private boolean test_resistance_flag = true;
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
	//private Switch mySwitch;
	private static final String MyPREFERENCES = "Calibration" ;
	static final short ALPHA = (short) 0.25f; // if ALPHA = 1 OR 0, no filter applies.
	SharedPreferences sharedpreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
	            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING); 
		
		sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		
		
		float left_max = sharedpreferences.getFloat("leftMax", 0.0f);
		float right_max = sharedpreferences.getFloat("rightMax", 0.0f);


		
		if(left_max != 0.0f && right_max != 0.0f){
			runOnUiThread(new Runnable() {
                public void run() {
                	TextView StatusContent = (TextView) findViewById(R.id.status_content);
		    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
		    		StatusContent.setText("Ready");
		    		StatusDetails
		    				.setText("Calibration done. Please press Test button to start sensing or press Calibrate button to calibrate again");	 

                }
            });		
		}
		
		else if(left_max == 0.0f){
			runOnUiThread(new Runnable() {
                public void run() {
                	TextView StatusContent = (TextView) findViewById(R.id.status_content);
		    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
		    		StatusContent.setText("Calibrate Left");
		    		StatusDetails
		    				.setText("Press Calibrate button to first calibrate the left channel. Make sure the sensor is connected to the headset interface.");	 

                }
            });		
		}
		
		else if(left_max !=0.0f&&right_max ==0.0f){
			runOnUiThread(new Runnable() {
                public void run() {
                	TextView StatusContent = (TextView) findViewById(R.id.status_content);
		    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
		    		StatusContent.setText("Calibrate Right");
		    		StatusDetails
		    				.setText("Press Calibrate button to calibrate the right channel. Make sure the sensor is connected to the headset interface.");	 

                }
            });		
		}
		
		/**mySwitch = (Switch) findViewById(R.id.mySwitch);
		
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
			});**/
		   
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
	public void calibrate(View view){
		TextView StatusContent = (TextView) findViewById(R.id.status_content);
		String status = StatusContent.getText().toString();
		if(status.equalsIgnoreCase("calibrate left")){
			leftCalibration(view);
			runOnUiThread(new Runnable() {
				public void run() {					
					Toast.makeText(getApplicationContext(), "Calibrating the left channel",  Toast.LENGTH_LONG).show();

				}
			});
		}
		
		else if(status.equalsIgnoreCase("calibrate right")){
			rightCalibration(view);
			runOnUiThread(new Runnable() {
				public void run() {					
					Toast.makeText(getApplicationContext(), "Calibrating the right channel",  Toast.LENGTH_LONG).show();

				}
			});
		}
		
		
		else if(status.equalsIgnoreCase("ready")){
			runOnUiThread(new Runnable() {
				public void run() {			
					Toast.makeText(getApplicationContext(), "You may now calibrate again",  Toast.LENGTH_LONG).show();
			 		TextView StatusContent = (TextView) findViewById(R.id.status_content);
		    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
		    		StatusContent.setText("Calibrate Left");
		    		StatusDetails
		    				.setText("Press Calibrate button to first calibrate the left channel. Make sure the sensor is connected to the headset interface.");	 
				}
			});
		}
	}
    
	
	public void test_resistance(View view){
		test_resistance_flag = true;
		start_stereo(view);
	}
	
	public void test_temperature(View view){
		test_resistance_flag = false;
		start_stereo(view);
	}
	

	public void start_stereo(View view){
		EditText offsetText = (EditText) findViewById(R.id.offset);
		String offset = offsetText.getText().toString();
		final byte offset_lf = Byte.valueOf(offset);
		EditText freqText = (EditText) findViewById(R.id.input_freq);
		String freq = freqText.getText().toString();
		stereo_frequency = Float.valueOf(freq);
		sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		float left_max = sharedpreferences.getFloat("leftMax", 0.0f);
		float right_max = sharedpreferences.getFloat("rightMax", 0.0f);
	
		if(left_max == 0.0f || right_max == 0.0f){
			runOnUiThread(new Runnable() {
                public void run() {
            //Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)ohm, Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Please calibrate the channels first.", Toast.LENGTH_SHORT).show();

                }
            });
			return;
		}		
		Rint = (10000*right_max - 2000*left_max)/(left_max-right_max);
        K = (right_max*Rint + 10000*right_max)/Rint;
        runOnUiThread(new Runnable() {
            public void run() {
        //Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)ohm, Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "Retrive Rint: " + Rint +" K: "+ K,  Toast.LENGTH_SHORT).show();
            }
        });
        
        

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
	            AudioDeviceStereoDuo device = new AudioDeviceStereoDuo();
	            float samples[] = new float[1024];
	            device.changeTheOffset(offset_lf);
	            
	            runOnUiThread(new Runnable() {
					public void run() {
						Button handle = null;
						if(test_resistance_flag){
						handle = (Button) findViewById(R.id.inner_start_stereo);
						}
						else{
							handle = (Button) findViewById(R.id.inner_start_stereo2);	
						}
						handle.setVisibility(View.INVISIBLE);
						if(test_resistance_flag){
						handle = (Button) findViewById(R.id.inner_stop_stereo);
						}
						else{
							handle = (Button) findViewById(R.id.inner_stop_stereo2);	
						}
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
	            //sData_lowpass = noisy_filter(sData,sData_lowpass);
	            
	           /** for(int i =0; i< sData_lowpass.length; i++){
	            	if(max < Math.abs((int)sData_lowpass[i]))
	            		max = Math.abs((int)sData_lowpass[i]);            		
	            }	**/
	            int counter = 0;
	            for(int i = 1; i<sData.length-1; i++){
	            	if(sData[i-1]<sData[i] && sData[i]>sData[i+1]){
	            		max+=sData[i];
	            		counter++;
	            	}
	            }
	            max/=counter;
	            //put the fit calculation here
	            
	            final double Rx = Rint*K/max - Rint - 2000;
	            
	            runOnUiThread(new Runnable() {
	                public void run() {
	            Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)Rx + " DAC: " + max, Toast.LENGTH_SHORT).show();
	            
	                }
	            }); 
	            
	            //Visualize the estimated value depends on test_resistance_flag
	    		TextView resistance_result = (TextView) findViewById(R.id.resistance_result);
	    		TextView temp_result = (TextView) findViewById(R.id.temp_result);

	    		
	    		if(test_resistance_flag){
	    			resistance_result.setText("Estimated resistance is: ");
	    			resistance_result.setTextColor(Color.CYAN);
	    			temp_result.setTextColor(Color.BLACK);
	    			//TO ADD formula to calculate resistance.
	    			write2CSV(true, "1");
	    		}
	    		else{
	    			temp_result.setText("Estimated temperature is: ");	
	    			temp_result.setTextColor(Color.CYAN);
	    			resistance_result.setTextColor(Color.BLACK);
	    			//TO ADD formula to calculate temperature.
	    			write2CSV(false, "2");
	    		}
	            
	            
	         }         
	      } ).start();	
		
		
		
		
	}
	
	public void write2CSV(boolean test_resistance, String val){
			String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
			String fileName = "Measured_data.csv";
			String filePath = baseDir + File.separator + fileName;
			File f = new File(filePath );
			CSVWriter writer = null; 
			FileWriter mFileWriter = null;
			// File exist
			if(f.exists() && !f.isDirectory()){
				try {
					mFileWriter = new FileWriter(filePath , true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				writer = new CSVWriter(mFileWriter);
			}
			else {
				try {
					writer = new CSVWriter(new FileWriter(filePath));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");	
			Date curDate = new Date();
			String DateToStr = format.format(curDate);
			String type = null;
			if(test_resistance){
				type = "Test Resistance Value";
			}
			else{
				type = "Test Temperature Value";
			}
			String[] data = {"Date",DateToStr, type, val};

			writer.writeNext(data);			
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
	public void leftCalibration(View view){
		EditText offsetText = (EditText) findViewById(R.id.offset);
		String offset = offsetText.getText().toString();
		final byte offset_lf = Byte.valueOf(offset);
		EditText freqText = (EditText) findViewById(R.id.input_freq);
		String freq = freqText.getText().toString();
		stereo_frequency = Float.valueOf(freq);
		sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		final int calibration_time = 5000; //10seconds
		new Thread( new Runnable( ) 
	      {
	         public void run( )
	         {        		
	           
	            float increment = (float)(2*Math.PI) * stereo_frequency / samplingRate; // angular increment for each sample
	            float angle = 0;
	            //AndroidAudioDevice device = new AndroidAudioDevice( );
	            AudioDeviceStereo device = new AudioDeviceStereo();
	            float samples[] = new float[1024];
	            device.changeTheOffset(offset_lf);
	            StartRecord();
	            long startTime = System.currentTimeMillis(); //fetch starting time
	            while( (System.currentTimeMillis()-startTime)< calibration_time )
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
	           /** sData_lowpass = noisy_filter(sData,sData_lowpass);
	            
	            for(int i =0; i< sData_lowpass.length; i++){
	            	if(max < Math.abs((int)sData_lowpass[i]))
	            		max = Math.abs((int)sData_lowpass[i]);            		
	            }**/
	            int counter = 0;
	            for(int i = 1; i<sData.length-1; i++){
	            	if(sData[i-1]<sData[i] && sData[i]>sData[i+1]){
	            		max+=sData[i];
	            		counter++;
	            	}
	            }
	            max/=counter;
	          
	            
	            left_max = max;	            
	            //Rint = (10000*right_max - 2000*left_max)/(left_max-right_max);
	            //K = (right_max*Rint + 10000*right_max)/Rint;
	            SharedPreferences.Editor editor = sharedpreferences.edit();
	            editor.putFloat("leftMax",(float) left_max);	          
	            editor.commit();
	            
	            runOnUiThread(new Runnable() {
	                public void run() {
	            //Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)ohm, Toast.LENGTH_SHORT).show();
	            Toast.makeText(getApplicationContext(), "Calibration done. Left max is: " + left_max,  Toast.LENGTH_LONG).show();
	            TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Calibrate Right");
	    		StatusDetails
	    				.setText("Press Calibrate button to calibrate the right channel. Make sure the sensor is connected to the headset interface.");	 
	                }
	            });
	         }         
	      } ).start();
		
		
		
	}
	
	public void rightCalibration(View view){
		EditText offsetText = (EditText) findViewById(R.id.offset);
		String offset = offsetText.getText().toString();
		final byte offset_lf = Byte.valueOf(offset);
		EditText freqText = (EditText) findViewById(R.id.input_freq);
		String freq = freqText.getText().toString();
		stereo_frequency = Float.valueOf(freq);
		sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		final int calibration_time = 5000; //10seconds
		new Thread( new Runnable( ) 
	      {
	         public void run( )
	         {        		
	           
	            float increment = (float)(2*Math.PI) * stereo_frequency / samplingRate; // angular increment for each sample
	            float angle = 0;
	            //AndroidAudioDevice device = new AndroidAudioDevice( );
	            AudioDeviceStereoRight adsr = new AudioDeviceStereoRight();
	            float samples[] = new float[1024];
	            adsr.changeTheOffset(offset_lf);
	          
	            StartRecord();
	            long startTime = System.currentTimeMillis(); //fetch starting time
	            while( (System.currentTimeMillis()-startTime)< calibration_time )
	            {
	               for( int i = 0; i < samples.length; i++ )
	               {
	                  samples[i] = (float)Math.sin( angle );
	                  angle += increment;
	               }
	 
	               adsr.writeSamples( samples );
	            }        	
	            stopRecording();            
	            adsr.release();
	         
	            max = 0;
	            //low pass filter
	            int counter = 0;
	            for(int i = 1; i<sData.length-1; i++){
	            	if(sData[i-1]<sData[i] && sData[i]>sData[i+1]){
	            		max+=sData[i];
	            		counter++;
	            	}
	            }
	            max/=counter;

	            right_max = max;
	            
	            //Rint = (10000*right_max - 2000*left_max)/(left_max-right_max);
	            //K = (right_max*Rint + 10000*right_max)/Rint;
	            SharedPreferences.Editor editor = sharedpreferences.edit();
	            editor.putFloat("rightMax",(float) right_max);
	         
	            editor.commit();
	            
	            runOnUiThread(new Runnable() {
	                public void run() {
	            //Toast.makeText(getApplicationContext(), "Estimated resistance is "+ (int)ohm, Toast.LENGTH_SHORT).show();
	            Toast.makeText(getApplicationContext(), "Calibration done. right max is: " + right_max, Toast.LENGTH_LONG).show();
	            TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Ready");
	    		StatusDetails
	    				.setText("Calibration done. Please press Test button to start sensing or press Calibrate button to calibrate again");	 
	            
	            
	            
	            
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
	
	public void stop_stereo(View view){
		
		mStop = false;
		
		runOnUiThread(new Runnable() {
			public void run() {
				Button handle = null;
				if(test_resistance_flag){
				handle = (Button) findViewById(R.id.inner_start_stereo);
				}
				else{
				handle = (Button) findViewById(R.id.inner_start_stereo2);	
				}
				handle.setVisibility(View.VISIBLE);
				if(test_resistance_flag){
				handle = (Button) findViewById(R.id.inner_stop_stereo);
				}
				else{
					handle = (Button) findViewById(R.id.inner_stop_stereo2);	
				}
				handle.setVisibility(View.INVISIBLE);
				
				TextView StatusContent = (TextView) findViewById(R.id.status_content);
	    		TextView StatusDetails = (TextView) findViewById(R.id.status_details);
	    		StatusContent.setText("Ready");
	    		StatusDetails
	    				.setText("Calibration done. Please press Test button to start sensing or press Calibrate button to calibrate again");	
			}
		});
	}
	
	

	

	

	public void StartRecord(){
		
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


