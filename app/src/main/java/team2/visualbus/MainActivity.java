//This file is part of TakeYourBus.
//
//        TakeYourBus is free software: you can redistribute it and/or modify
//        it under the terms of the GNU Lesser General Public License as published by
//        the Free Software Foundation, either version 3 of the License, or
//        (at your option) any later version.
//
//        TakeYourBus is distributed in the hope that it will be useful,
//        but WITHOUT ANY WARRANTY; without even the implied warranty of
//        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//        GNU Lesser General Public License for more details.
//
//        You should have received a copy of the GNU Lesser General Public License
//        along with TakeYourBus.  If not, see <http://www.gnu.org/licenses/>.

package team2.visualbus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static team2.visualbus.R.id.mResult;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openCamera();
    }

    private void openCamera(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,70 , outStream);

            System.out.println("Pic taken");
            ImageView mImageView = (ImageView) this.findViewById(R.id.mImageView);
            mImageView.setImageBitmap(imageBitmap);
            visualFunction(convertToFile(outStream));
        }
    }

    private void visualFunction(File mFile){
        new ClassifyMePls(mFile).execute();
    }

    private File resizeImage(File mFileToResize){
        File mFile = null;

        Bitmap b = BitmapFactory.decodeFile(mFileToResize.getAbsolutePath());
        // original measurements
        int origWidth = b.getWidth();
        int origHeight = b.getHeight();

        final int destWidth = 600;//or the width you need

        if(origWidth > destWidth){
            // picture is wider than we want it, we calculate its target height
            int destHeight = origHeight/( origWidth / destWidth ) ;
            // we create an scaled bitmap so it reduces the image, not just trim it
            Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b2.compress(Bitmap.CompressFormat.JPEG,70 , outStream);

            return convertToFile(outStream);
        }
        else
            return mFileToResize;
    }

    private File convertToFile(ByteArrayOutputStream outStream){
        // we save the file, at least until we have made use of it
        File mFile = new File(Environment.getExternalStorageDirectory()
                + File.separator + "test.jpg");

        try {
            mFile.createNewFile();
            //write the bytes in file
            FileOutputStream fo = new FileOutputStream(mFile);
            fo.write(outStream.toByteArray());
            // remember close de FileOutput
            fo.close();
        }catch(Exception mException){
            mException.printStackTrace();
        }
        return mFile;
    }

    class ClassifyMePls extends AsyncTask<Void, Void, String> {

        File mFileToAnalyze;
        public ClassifyMePls(File mFile){
            super();
            mFileToAnalyze = mFile;
        }

        @Override
        protected String doInBackground(Void... arg0) {
            VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
            service.setApiKey("6606b68d29b2d6f2669ee6351311f815ccd009dd");

            System.out.println("Check file");
            System.out.println(mFileToAnalyze.exists());
            System.out.println(mFileToAnalyze == null);

            //The classifier is already trained, we just recover it
            VisualClassifier mClassifier = service.getClassifier("ruta_78_2029812623").execute();

            System.out.println("Getting image to classify");
            File mFileToClassify = mFileToAnalyze;
            mFileToClassify = resizeImage(mFileToClassify);

            System.out.println("Preparing ClassifyImagesOptions");
            ClassifyImagesOptions.Builder mClassImageOptsBldr = new ClassifyImagesOptions.Builder();
            mClassImageOptsBldr.images(mFileToClassify);
            mClassImageOptsBldr.classifierIds(mClassifier.getId());
            ClassifyImagesOptions mClassImageOpts = mClassImageOptsBldr.build();

            ServiceCall<VisualClassification> mResult = service.classify(mClassImageOpts);
            System.out.println("Final result!!! : ");
            String mResultAsString = mResult.execute().toString();
            return mResultAsString;
        }

        TextView mWatsonTextView = (TextView) findViewById(mResult);

        protected void onPostExecute(String mWatsonResult) {
            try {
                System.out.println(mWatsonResult);
                JSONObject jObject = new JSONObject(mWatsonResult);
                JSONArray mJSONArray = jObject.getJSONArray("images");

                try {
                    JSONObject oneObject = mJSONArray.getJSONObject(0);

                    System.out.println("Decomposing: ");
                    mJSONArray = oneObject.getJSONArray("classifiers");
                    JSONObject mJSONObject = null;
                    if(mJSONArray.length() > 0) {
                        mJSONObject = oneObject.getJSONArray("classifiers")
                                .getJSONObject(0).getJSONArray("classes")
                                .getJSONObject(0);
                    }
                    String mResult;
                    if(mJSONObject != null && mJSONObject.getDouble("score") > 0.5)
                        mResult = "bus 78";
                    else
                        mResult = "No bus identified";
                    mWatsonTextView.setText(mResult);
                    TextToSpeech ttswsh = new TextToSpeech(getApplicationContext());
                    ttswsh.execute(mResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }catch(Exception mException){
                mException.printStackTrace();
            }
        }

    }

}
