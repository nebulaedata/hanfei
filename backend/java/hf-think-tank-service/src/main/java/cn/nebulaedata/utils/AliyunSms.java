package cn.nebulaedata.utils;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import org.jetbrains.annotations.NotNull;
import com.google.gson.Gson;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import darabonba.core.client.ClientOverrideConfiguration;


/**
 * @ClassName AliyunSms
 * @Description 阿里云发送短信，<a href="https://dysms.console.aliyun.com/overview">申请地址</a>
 * @Author longlongago2
 */
public class AliyunSms {
    private final String signName; // 短信签名名称
    private final String templateCode; // 短信模板Code

    public static void main(String[] args) throws Exception {
    }

    public AliyunSms(String signName, String templateCode) {
        this.signName = signName;
        this.templateCode = templateCode;
    }

    /**
     * 静态方法：生成随机验证码
     *
     * @param length 验证码长度
     * @return 验证码
     */
    public static @NotNull String generatorCode(Integer length) {
        return String.valueOf((int) ((Math.random() * 9 + 1) * Math.pow(10, length - 1)));
    }

    /**
     * 发送短信
     *
     * @param phoneNumbers  电话号码：支持对多个手机号码发送短信，手机号码之间以半角逗号（,）分隔。上限为1000个手机号码
     * @param templateParam 模板参数，例如：{"code":"123456"}
     * @return 发送结果Json字符串
     */
    public String send(String phoneNumbers, Object templateParam) throws Exception {

        String accessKeySecret = "";
        String accessKeyId = "";

        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build());

        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.create()
                .setEndpointOverride("dysmsapi.aliyuncs.com")
                .setConnectTimeout(Duration.ofSeconds(30));

        AsyncClient client = AsyncClient.builder()
                .region("cn-hangzhou")
                .credentialsProvider(provider)
                .overrideConfiguration(clientConfig)
                .build();

        // Parameter settings for API request
        SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                .signName(this.signName)
                .templateCode(this.templateCode)
                .templateParam(new Gson().toJson(templateParam))
                .phoneNumbers(phoneNumbers)
                .build();

        // Asynchronously get the return value of the API request
        CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);

        // Synchronously get the return value of the API request
        SendSmsResponse resp = response.get();

        String res = new Gson().toJson(resp);

        // Finally, close the client
        client.close();

        return res;
    }
}


