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

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.ibm.watson.developer_cloud.text_to_speech.v1.model.AudioFormat;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by Alejandro Vargas on 5/18/2017.
 */

public class TextToSpeech {
    private final String WEBSERVICE_URL = "https://stream.watsonplatform.net/text-tospeech/api/v1/synthesize";
    private final String USERNAME = "38652742-807f-4e5a-93b7-9c4d270c10e2";
    private final String PASSWORD = "6vSejxqCB7Qf";
    private String type = "POST";
    private Context appContext;
    private File convertedFile;
    private String inputText;

    public TextToSpeech(Context appContext) {
        this.appContext = appContext;
    }
    public void execute(String inputText) {
        this.inputText = inputText;
        new TheTask().execute(WEBSERVICE_URL);
    }
    class TheTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... arg0) {
            String text = null;
            try {
                com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech service = new
                        com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech();
                service.setUsernameAndPassword(USERNAME, PASSWORD);
                try {
                    text = inputText;
                    InputStream stream = service.synthesize(text, Voice.EN_ALLISON,
                            AudioFormat.OGG_VORBIS).execute();
                    File downloadsFolder =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                    File.createTempFile("convertedFile", ".ogg", downloadsFolder);
                    convertedFile = new File(downloadsFolder, "test.ogg");
                    FileOutputStream out = new FileOutputStream(convertedFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = stream.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    out.close();
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }
        public void playFile() {
            try {
                FileInputStream fis = new FileInputStream(convertedFile);
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(fis.getFD());
                mp.prepare();
                mp.start();
            } catch (Exception e) {
                Log.e("MEDIA", e.toString());
                e.printStackTrace();
            }
        }//end playFile
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            playFile();
        }
    }
}
