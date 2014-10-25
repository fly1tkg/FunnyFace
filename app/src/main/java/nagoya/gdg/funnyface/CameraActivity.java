package nagoya.gdg.funnyface;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends Activity {
	private static final String TAG = "CameraActivity";
	Preview preview;
	Button buttonClick;
	Camera camera;
	Activity act;
	Context ctx;
    private TextView mTextView;
    private int mState = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
        mTextView = (TextView) findViewById(R.id.textView);

		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((RelativeLayout) findViewById(R.id.layout)).addView(preview);
		preview.setKeepScreenOn(true);

		preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});

		Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		int numCams = Camera.getNumberOfCameras();
		if(numCams > 0){
			try{
				camera = Camera.open(0);
                camera.setDisplayOrientation(90);
				camera.startPreview();
				preview.setCamera(camera);
			} catch (RuntimeException ex){
				Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			//			 Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - jpeg");

            if (mState == 0) {
                new SaveImageTask(true).execute(data);
                mTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                mTextView.setText("変顔を撮影してください");
                mState = 1;
            } else {
                new SaveImageTask(false).execute(data);
                mTextView.setTextColor(getResources().getColor(android.R.color.black));
                mTextView.setText("普通の顔を撮影してください");
                mState = 0;
            }
            resetCam();
        }
	};

    private File mNormalPhoto;

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        ProgressDialog dialog;
        boolean normal;
        FunnyFace result;


        SaveImageTask(boolean normal) {
            this.normal = normal;
        }

        @Override
        protected void onPreExecute() {
            if (!normal) {
                dialog = new ProgressDialog(CameraActivity.this);
                dialog.setMessage("変顔度を検出しています");
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File(sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);

                if (normal) {
                    mNormalPhoto = outFile;
                } else {
                    result = new FunnyFace(CameraActivity.this, mNormalPhoto, outFile);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            if (!normal) {
                dialog.dismiss();
                result.getMessageDialog().show();
            }
        }
    }
}


