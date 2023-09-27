package community;

import java.io.IOException;

public class WkTest {
    public static void main(String[] args) {
        String cmd = "D:/Program Files/wkhtmltopdf/bin/wkhtmltoimage  --quality 75 www.nowcoder.com d:/work/data/wk-image/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("Ok");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
