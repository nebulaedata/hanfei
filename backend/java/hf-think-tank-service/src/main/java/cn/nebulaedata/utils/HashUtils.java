package cn.nebulaedata.utils;

/**
 * @author 徐衍旭
 * @date 2021/3/5 14:09
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
public class HashUtils {

    /**
     * key取hash后 取模
     *
     * @param key
     * @return
     */
    public static String getHashMod(String key, Integer divisor) {
        return String.format("%03d", key.hashCode() & Integer.MAX_VALUE % divisor);
    }

    /**
     * key取hash后 256取模
     *
     * @param key
     * @return
     */
    public static String getHashMod(String key) {
        return getHashMod(key, 256);
    }


    public static void main(String[] args) {
//        String hashMod1 = getHashMod("31563ba588cd4031ba2a68f80d23fa26b76fbe5dc8aa4b17a04d92be4d79fbdc");  // 059
//        String hashMod2 = getHashMod("53208c54e3814d65b369de4b62604d5b61087ec22c92477181accc17d0d98f4f");
//        String hashMod3 = getHashMod("6750aed073054c63bf68fe1e400f88c68bdc2515acd34ad5adb2e2cdcb20eb8d");
//        String hashMod4 = getHashMod("8cdb5d5e32d04a1daed5d25a961fbab268024e09072e4b50bea99244dc97f631");
//        String hashMod5 = getHashMod("b74e777f5a454d1aa6199c87b75804e422d38898a75d48eab47f3c5e250ca6cc");
//        String hashMod6 = getHashMod("31563ba588cd4031ba2a68f80d23fa26dde4b54bb0cb411a881f60a6983b848d");
//        String hashMod7 = getHashMod("53208c54e3814d65b369de4b62604d5bf6d5928d1f6f49268960c0bf9635e4b3");
//        String hashMod8 = getHashMod("6750aed073054c63bf68fe1e400f88c6eca52b0c81f84ff1ad30cb9b9f470285");
//        String hashMod9 = getHashMod("b74e777f5a454d1aa6199c87b75804e46c00f471ba12405faf7b59999e5430ce");
//        System.out.println(hashMod1);
//        System.out.println(hashMod2);
//        System.out.println(hashMod3);
//        System.out.println(hashMod4);
//        System.out.println(hashMod5);
//        System.out.println(hashMod6);
//        System.out.println(hashMod7);
//        System.out.println(hashMod8);
//        System.out.println(hashMod9);
    }
}
