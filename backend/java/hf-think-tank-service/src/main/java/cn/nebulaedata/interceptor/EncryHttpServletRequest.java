//package cn.nebulaedata.interceptor;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletRequestWrapper;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//
///**
// * @author 贾亦真
// * @date 2022/12/6 13:40
// * @note
// * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
// */
//public class EncryHttpServletRequest extends HttpServletRequestWrapper {
//
//    private String body;
//
//    public EncryHttpServletRequest(HttpServletRequest request) {
//        super(request);
//        StringBuilder stringBuilder = new StringBuilder();
//        BufferedReader bufferedReader = null;
//        InputStream inputStream = null;
//        try {
//            inputStream = request.getInputStream();
//            if (inputStream != null) {
//                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//                char[] charBuffer = new char[128];
//                int bytesRead = -1;
//                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
//                    stringBuilder.append(charBuffer, 0, bytesRead);
//                }
//            } else {
//                stringBuilder.append("");
//            }
//        } catch (IOException ex) {
//        } finally {
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (bufferedReader != null) {
//                try {
//                    bufferedReader.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        body = stringBuilder.toString();
//    }
//
//    @Override
//    public BufferedReader getReader() throws IOException {
//        return new BufferedReader(new InputStreamReader(this.getInputStream()));
//    }
//
//    public String getBody() {
//        return body;
//    }
//
//    public void setBody(String body) {
//        this.body = body;
//    }
//}
