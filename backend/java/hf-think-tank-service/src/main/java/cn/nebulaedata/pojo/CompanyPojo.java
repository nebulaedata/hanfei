package cn.nebulaedata.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 16:21
 * @note
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class CompanyPojo {
    private String companyId;
    private String companyName;
    private String taxNumber;
    private String address;
    private String postCode;
    private String dateEstablish;
    private String registeredCapital;
    private String legalPersonName;
    private String contactPersonName;
    private String contactPersonTitle;
    private String businessLicense;
    private String bankName;
    private String phoneNumber;
    private String faxNumber;
    private String bankAccount;
    private String bankLicense;
    private List<Map<String, String>> shareholderNameList;
    private String employeesNumber;
    private String projectManagerNumber;
    private String senior;
    private String intermediate;
    private String primary;
    private String fullTimePerson;
    private String website;
    private String authorizationCode;
    private String createUserId;
    private String updateUserId;
    private Date createTime;
    private Date updateTime;

    /**
     * 非表字段
     */
    private String shareholderNameListStr;
//    private MultipartFile businessLicenseFile;
//    private MultipartFile bankLicenseFile;
    private boolean isMainCompany;  // 默认值false
    private String businessLicenseName;
    private String bankLicenseName;
}
