package dk.ilios.spanner.example;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

    /**
     * Copy a file from assets to the apps internal cache dir.
     *
     * @param fileName filename to copy.
     * @return a reference to the copied file.
     */
    public static File copyFromAssets(String fileName) {
        AssetManager am = MyApplication.getContext().getAssets();
        InputStream inputStream;
        try {
            inputStream = am.open("baseline.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return createFileFromInputStream(fileName, inputStream);
    }

    private static File createFileFromInputStream(String fileName, InputStream inputStream) {
        try {
            File f = new File(MyApplication.getContext().getCacheDir(), fileName);
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
   }
}
