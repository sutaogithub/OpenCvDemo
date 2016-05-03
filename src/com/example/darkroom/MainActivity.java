package com.example.darkroom;


import java.io.IOException;
import java.util.Arrays;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "man";
	private static final int SELECT_PICTURE = 1;
	private String selectedImagePath;
	static {
		if (!OpenCVLoader.initDebug()) {
			// Handle initialization error
		} else {
			// System.loadLibrary("detection_based_tracker");
		}
	}
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
	private Mat sampledImage;
	private Mat greyImage;
	private ImageView iv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		iv = (ImageView) findViewById(R.id.IODarkRoomImageView);
		initEvent();
	}

	private void initEvent() {
		// TODO Auto-generated method stub
		iv.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(sampledImage==null)
					return true;
				if(event.getAction()==MotionEvent.ACTION_DOWN){
					Toast.makeText(getApplicationContext(), "修复", 0).show();
					float x=event.getX();
					float y=event.getY();
					final int col=(int) (sampledImage.cols()*x/iv.getWidth());
					final int row=(int) (sampledImage.rows()*y/iv.getHeight());
					new Thread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							final Mat res=inpaint(sampledImage,col,row,5);
							
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									displayImage(res);
								}
							});
							
						}
					}).start();
					
				}
				
				return true;
			}
		});
	}
	
	private Mat inpaint(Mat src,int x,int y,int radius){
//		Mat tmp=new Mat();
//		Mat grey=new Mat();
		Mat res=new Mat();
//		Imgproc.cvtColor(src, grey, Imgproc.COLOR_RGB2GRAY);
		Rect r=new Rect(x-radius,y-radius,2*radius,2*radius);
		Mat mask =Mat.zeros(src.size(),CvType.CV_8UC1);
		mask.submat(r).setTo(new Scalar(255));
		Photo.inpaint(src, mask, res, 3, Photo.INPAINT_TELEA);
//		Imgproc.cvtColor(tmp, res, Imgproc.COLOR_GRAY2RGB);
		return res;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this,
		// mLoaderCallback);
		mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
		if (id == R.id.action_openGallary) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
			return true;
		} else if (sampledImage == null) {
			Context context = getApplicationContext();
			CharSequence text = "You need to load an image first!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		}
		if (id == R.id.action_Hist) {

			Mat histImage = new Mat();
			sampledImage.copyTo(histImage);
			calcHist(histImage);
			displayImage(histImage);
			return true;
		} else if (id == R.id.action_togs) {
			greyImage = new Mat();
			Imgproc.cvtColor(sampledImage, greyImage, Imgproc.COLOR_RGB2GRAY);
			displayImage(greyImage);
			return true;
		} else if (id == R.id.action_egs) {
			Mat eqGS = new Mat();
			Imgproc.equalizeHist(greyImage, eqGS);
			displayImage(eqGS);
			return true;
		} else if (id == R.id.action_HSV) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Mat V = new Mat(sampledImage.rows(), sampledImage.cols(), CvType.CV_8UC1);
					Mat S = new Mat(sampledImage.rows(), sampledImage.cols(), CvType.CV_8UC1);
					Mat HSV = new Mat();
					Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
					byte[] Vs = new byte[3];
					byte[] vsout = new byte[1];
					byte[] ssout = new byte[1];
					for (int i = 0; i < HSV.rows(); i++) {
						for (int j = 0; j < HSV.cols(); j++) {
							HSV.get(i, j, Vs);
							V.put(i, j, new byte[] { Vs[2] });
							S.put(i, j, new byte[] { Vs[1] });
						}
					}
					Imgproc.equalizeHist(V, V);
					Imgproc.equalizeHist(S, S);
					for (int i = 0; i < HSV.rows(); i++) {
						for (int j = 0; j < HSV.cols(); j++) {
							V.get(i, j, vsout);
							S.get(i, j, ssout);
							HSV.get(i, j, Vs);
							Vs[2] = vsout[0];
							Vs[1] = ssout[0];
							HSV.put(i, j, Vs);
						}
					}
					final Mat enhancedImage = new Mat();
					Imgproc.cvtColor(HSV, enhancedImage, Imgproc.COLOR_HSV2RGB);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(enhancedImage);

						}
					});
				}
			}).start();

			return true;
		} else if (id == R.id.action_ER) {

			new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Mat redEnhanced = new Mat();
					sampledImage.copyTo(redEnhanced);
					//通过修改Scalar的四个参数前三个时rgb顺序不顶，最后一个时a
					Mat redMask = new Mat(sampledImage.rows(), sampledImage.cols(), sampledImage.type(),
							new Scalar(1, 0, 0, 0));
					enhanceChannel(sampledImage, redMask);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(sampledImage);

						}
					});
				}
			}).start();

		}else if(id==R.id.action_average)
		{
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat blurredImage=new Mat();
					Size size=new Size(7,7);
					Imgproc.blur(sampledImage, blurredImage, size);
				
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(blurredImage);
						}
					});
				}
				
			}).start();
		
			return true;
		} else if(id==R.id.action_gaussian){
			/* code to handle the user not loading an image**/
			/**/
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat blurredImage=new Mat();
					Size size=new Size(7,7);
					Imgproc.GaussianBlur(sampledImage, blurredImage, size,0,0);
					runOnUiThread(new  Runnable() {
						public void run() {
							displayImage(blurredImage);

							
						}
					});
				}
				
			}).start();
			
			return true;
		} else if(id==R.id.action_median){
			/* code to handle the user not loading an image**/
			/**/
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat blurredImage=new Mat();
					int kernelDim=7;
					Imgproc.medianBlur(sampledImage,blurredImage , kernelDim);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(blurredImage);

						}
					});
				}
				
			}).start();
			
			return true;
		}else if(id==R.id.action_harris){
			/* code to handle the user not loading an image**/
			/**/
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat greyImage=new Mat();
					MatOfKeyPoint keyPoints=new MatOfKeyPoint();
					Imgproc.cvtColor(sampledImage, greyImage, Imgproc.COLOR_RGB2GRAY);
					FeatureDetector detector = FeatureDetector.create(FeatureDetector.HARRIS);
					detector.detect(greyImage, keyPoints);
					Features2d.drawKeypoints(greyImage, keyPoints, greyImage);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(greyImage);

						}
					});
				}
				
			}).start();
			
			return true;
		}else if(id==R.id.action_fast)
		{
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat greyImage=new Mat();
					MatOfKeyPoint keyPoints=new MatOfKeyPoint();
					Imgproc.cvtColor(sampledImage, greyImage, Imgproc.COLOR_RGB2GRAY);
					FeatureDetector detector = FeatureDetector.create(FeatureDetector.FAST);
					detector.detect(greyImage, keyPoints);
					Features2d.drawKeypoints(greyImage, keyPoints, greyImage);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(greyImage);

						}
					});
				}
				
			}).start();
			
			return true;
		}else if(id==R.id.action_orb)
		{
			new Thread(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					final Mat greyImage=new Mat();
					MatOfKeyPoint keyPoints=new MatOfKeyPoint();
					Imgproc.cvtColor(sampledImage, greyImage, Imgproc.COLOR_RGB2GRAY);
					FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
					detector.detect(greyImage, keyPoints);
					Features2d.drawKeypoints(greyImage, keyPoints, greyImage);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							displayImage(greyImage);

						}
					});
				}
				
			}).start();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// 计算图像直方图
	private void calcHist(Mat image) {
		int mHistSizeNum = 25;
		MatOfInt mHistSize = new MatOfInt(mHistSizeNum);
		Mat hist = new Mat();
		float[] mBuff = new float[mHistSizeNum];
		MatOfFloat histogramRanges = new MatOfFloat(0f, 256f);
		Scalar mColorsRGB[] = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255),
				new Scalar(0, 0, 200, 255) };
		org.opencv.core.Point mP1 = new org.opencv.core.Point();
		org.opencv.core.Point mP2 = new org.opencv.core.Point();
		int thikness = (int) (image.width() / (mHistSizeNum + 10) / 3);
		if (thikness > 3)
			thikness = 3;
		MatOfInt mChannels[] = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
		Size sizeRgba = image.size();
		int offset = (int) ((sizeRgba.width - (3 * mHistSizeNum + 30) * thikness));
		// RGB
		for (int c = 0; c < 3; c++) {
			Imgproc.calcHist(Arrays.asList(image), mChannels[c], new Mat(), hist, mHistSize, histogramRanges);
			Core.normalize(hist, hist, sizeRgba.height / 2, 0, Core.NORM_INF);
			hist.get(0, 0, mBuff);
			for (int h = 0; h < mHistSizeNum; h++) {
				mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
				mP1.y = sizeRgba.height - 1;
				mP2.y = mP1.y - (int) mBuff[h];
				Imgproc.line(image, mP1, mP2, mColorsRGB[c], thikness);
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == SELECT_PICTURE) {
				Uri selectedImageUri = data.getData();
				selectedImagePath = getPath(selectedImageUri);
				Log.i(TAG, " : " + selectedImagePath);
				loadImage(selectedImagePath);
				displayImage(sampledImage);
			}
		}
	}

	private String getPath(Uri uri) {
		// just some safety built in
		if (uri == null) {
			return null;
		}
		// try to retrieve the image from the media store first
		// this will only work for images selected from gallery
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor != null) {
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}
		return uri.getPath();
	}

	private void loadImage(String path) {
		Mat originalImage = Imgcodecs.imread(path);
		Mat rgbImage = new Mat();
		Imgproc.cvtColor(originalImage, rgbImage, Imgproc.COLOR_BGR2RGB);
		Display display = getWindowManager().getDefaultDisplay();
		// This is "android graphics Point" class
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		sampledImage = new Mat();
		double downSampleRatio = calculateSubSampleSize(rgbImage, width, height);
		Imgproc.resize(rgbImage, sampledImage, new Size(), downSampleRatio, downSampleRatio, Imgproc.INTER_AREA);
		try {
			ExifInterface exif = new ExifInterface(selectedImagePath);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				// get the mirrored image
				sampledImage = sampledImage.t();
				// flip on the y-axis
				Core.flip(sampledImage, sampledImage, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				// get up side down image
				sampledImage = sampledImage.t();
				// Flip on the x-axis
				Core.flip(sampledImage, sampledImage, 0);
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double calculateSubSampleSize(Mat srcImage, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = srcImage.height();
		final int width = srcImage.width();
		double inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			// Calculate ratios of requested height and width to the raw
			// height and width
			final double heightRatio = (double) reqHeight / (double) height;
			final double widthRatio = (double) reqWidth / (double) width;
			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee final image with both dimensions larger than or
			// equal to the requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}

	private void displayImage(Mat image) {
		// create a bitMap
		Bitmap bitMap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
		// convert to bitmap:
		Utils.matToBitmap(image, bitMap);
		// find the imageview and draw it!
		if(iv==null)
			iv = (ImageView) findViewById(R.id.IODarkRoomImageView);
		iv.setImageBitmap(bitMap);
	}

	private void enhanceChannel(Mat imageToEnhance, Mat mask) {
		Mat channel = new Mat(sampledImage.rows(), sampledImage.cols(), CvType.CV_8UC1);
		sampledImage.copyTo(channel, mask);
		Imgproc.cvtColor(channel, channel, Imgproc.COLOR_RGB2GRAY, 1);
		Imgproc.equalizeHist(channel, channel);
		Imgproc.cvtColor(channel, channel, Imgproc.COLOR_GRAY2RGB, 3);
		channel.copyTo(imageToEnhance, mask);
	}
}
