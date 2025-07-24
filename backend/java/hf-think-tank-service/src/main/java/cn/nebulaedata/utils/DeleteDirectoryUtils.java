package cn.nebulaedata.utils;

import java.io.File;

public class DeleteDirectoryUtils {

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dir 将要删除的文件目录
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dirStr 将要删除的文件目录
     */
    public static boolean deleteDir(String dirStr) {
        File dir = new File(dirStr);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }        // 目录此时为空，可以删除
        return dir.delete();
    }

    /**
     * 测试
     */
    public static void main(String[] args) {
        String newDir2 = "C:\\Users\\xuyanxu\\Desktop\\1\\eefc4769c2b5449ebeaaf14a94111726.html";
        boolean success = deleteDir(new File(newDir2));
        if (success) {
            System.out.println("Successfully deleted populated directory: " + newDir2);
        } else {
            System.out.println("Failed to delete populated directory: " + newDir2);
        }
    }
}