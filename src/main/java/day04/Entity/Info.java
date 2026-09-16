package day04.Entity;


import lombok.Data;
import oracle.sql.CLOB;

import java.sql.Date;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-16
 * @Version: 1.0
 */

@Data
public class Info {
    private String SOURCE_NAME;
    private String DETAIL_LINK;
    private String DETAIL_TITLE;
    private CLOB DETAIL_CONTENT;
    private Date PAGE_TIME;
    private Date CREATE_TIME;
    private String LIST_TITLE;
    private String CREATE_BY;


}
