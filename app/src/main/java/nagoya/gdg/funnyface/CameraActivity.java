package nagoya.gdg.funnyface;

import android.app.Activity;
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


        SaveImageTask(boolean normal) {
            this.normal = normal;
        }

        @Override
        protected void onPreExecute () {
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
                    // TODO
                    Thread.sleep(2000);
                }
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
			}
			return null;
		}

        protected void onPostExecute (Void result) {
            if (!normal) {
                dialog.dismiss();
            }
        }

        private void cropFace(File normalFace, File funnyFace) {
            Bitmap faceBlack = BitmapFactory.decodeResource(getResources(), R.drawable.face_black);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;

            Bitmap normalOriginal = BitmapFactory.decodeFile(normalFace.getAbsolutePath(), options);
            Bitmap normal = Bitmap.createScaledBitmap(normalOriginal, 400, 640, false);
            normalOriginal.recycle();
            Canvas c = new Canvas(normal);
            c.drawBitmap(faceBlack, 0, 0, null);

            Bitmap funnyOriginal = BitmapFactory.decodeFile(funnyFace.getAbsolutePath(), options);
            Bitmap funny = Bitmap.createScaledBitmap(funnyOriginal, 400, 640, false);
            funnyOriginal.recycle();
            Canvas c1 = new Canvas(funny);
            c1.drawBitmap(faceBlack, 0, 0, null);
        }
	}

    private int convertGray(int dotColor) {
        float r = (float) Color.red(dotColor);
        float g = (float)Color.green(dotColor);
        float b = (float)Color.blue(dotColor);

        return (int)(r * 0.3 + g * 0.59 + b * 0.11);
    }

    public int diffImages(Bitmap image1, Bitmap image2) {

        if (image1.getWidth()!=image2.getWidth() || image1.getHeight()!=image2.getHeight()) {
            return -1;
        }

        int w = image1.getWidth();
        int h = image1.getHeight();

        int diff = 0;

        for (int i = 0; i < w; i++) {
            for (int j=0; j < h; j++) {
                int dotColor1 = image1.getPixel(i, j);
                int dotColor2 = image2.getPixel(i, j);

                int grayColor1 = convertGray(dotColor1);
                int grayColor2 = convertGray(dotColor2);

                diff += Math.abs(grayColor1 - grayColor2);
            }
        }

        return diff;
    }
}


