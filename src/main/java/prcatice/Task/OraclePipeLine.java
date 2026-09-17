package prcatice.Task;


import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * @Author: 岑正茂
 * @CreateTime: 2026-09-17
 * @Version: 1.0
 */


public class OraclePipeLine implements Pipeline {


    @Override
    public void process(ResultItems resultItems, Task task) {
        /*执行jdbc流程*/

        //1.获取resultItems元素

        //2.进行非空判断
        //将获取的元素通过set方式放入jdbc中，执行插入更新操作
    }
}
