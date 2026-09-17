package prcatice.POJO;

import lombok.Data;

import java.util.Date;

@Data
public class Info {
    private String SOURCE_NAME;
    private String DETAIL_LINK;
    private String DETAIL_TITLE;
    private String DETAIL_CONTENT;
    private Date PAGE_TIME;
    private Date CREATE_TIME;
    private String LIST_TITLE;
    private String CREATE_BY;
}