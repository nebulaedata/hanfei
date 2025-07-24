package cn.nebulaedata.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.*;

//import com.github.houbb.word.checker.util.ZhWordCheckers;

/**
 * @author 贾亦真
 * @date 2021/3/5 14:09
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
public class FileSizeUtils {

    /**
     * 根据java.nio.*的流获取文件大小
     * @param file
     */
    public static void getFileSize3(File file){
        FileChannel fc = null;
        try {
            if(file.exists() && file.isFile()){
                String fileName = file.getName();
                FileInputStream fis = new FileInputStream(file);
                fc = fis.getChannel();
                System.out.println("文件"+fileName+"的大小是："+fc.size()+"\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(null!=fc){
                try {
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    private long getTotalSizeOfFilesInDir(final ExecutorService service,
                                          final File file) throws InterruptedException, ExecutionException,
            TimeoutException {
        if (file.isFile())
            return file.length();

        long total = 0;
        final File[] children = file.listFiles();

        if (children != null) {
            final List<Future<Long>> partialTotalFutures = new ArrayList<Future<Long>>();
            for (final File child : children) {
                partialTotalFutures.add(service.submit(new Callable<Long>() {
                    public Long call() throws InterruptedException,
                            ExecutionException, TimeoutException {
                        return getTotalSizeOfFilesInDir(service, child);
                    }
                }));
            }

            for (final Future<Long> partialTotalFuture : partialTotalFutures)
                total += partialTotalFuture.get(100, TimeUnit.SECONDS);

        }

        return total;

    }

    public long getTotalSizeOfFile(final String fileName)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ExecutorService service = Executors.newFixedThreadPool(100);
        try {
            return getTotalSizeOfFilesInDir(service, new File(fileName));
        } finally {
            service.shutdown();
        }
    }

    public static String numberOfFiles(final String filePath){
        File folder = new File(filePath);
        File []list = folder.listFiles();
        int fileCount = 0, folderCount = 0;
        long length = 0;
        for (File file : list){
            if (file.isFile()){
                fileCount++;
                length += file.length();
            }else {
                folderCount++;
            }
        }
        System.out.println("文件夹的数目: " + folderCount + " 文件的数目: " + fileCount);
        return String.valueOf(fileCount);
    }

    public static void main(String[] args) throws Exception {
//        String Str = new String("Welcome to Yiibai.com");
//
//        System.out.print("Return Value :" );
//        System.out.println(Str.indexOf("Welcome") );
//
//        System.out.print("Return Value :" );
//        System.out.println(Str.indexOf("Tutorials") );
//
//        System.out.print("Return Value :" );
//        System.out.println(Str.indexOf("Yiibai", 11) );

//        final String right = "正确";
//        final String error = "万变不离其中";
//        String correct = ZhWordCheckers.correct(right);
//        String correct1 = ZhWordCheckers.correct(error);
//        System.out.println(correct);
//        System.out.println(correct1);

        getFileSize3(new File("C:\\Users\\xuyanxu\\Downloads\\GB 5135.7-2018 自动喷水灭火系统 第7部分：水流指示器.docx"));

        String fileName = "C:\\Users\\xuyanxu\\Desktop\\韩非";
        final long start = System.nanoTime();
        final long total = new FileSizeUtils()
                .getTotalSizeOfFile(fileName);
        final long end = System.nanoTime();
        System.out.println("Total Size: " + total);
        System.out.println("Time taken: " + (end - start) / 1.0e9);
    }
}
