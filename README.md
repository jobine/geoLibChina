# ChinaGeoLib
修改自： https://github.com/deng0515001/lnglat2Geo， 非常感谢原作者的贡献，我完全照搬了原作者的算法实现，但是我不能理解这些算法的原理。

修改点如下：

1. 使用java完全重写Scala代码，原因如下
   1. 作为lib，java和scala依赖是单项的，Scala程序员可以看懂java，但是java程序员不一定能看懂Scala
   2. 作为lib库，需要考虑库的精简，他引入了Scala的runtime，将会带来额外的jar包依赖
   3. GeoTrans的标准API并不是可以无缝提供给java端使用（部分API java无法调用，因为有很多scala的特性）
   4. scala语法糖很舒服，但是进行代码cr能发现很多带有性能风险的用法，以及Scala函数式上代码结构不清晰（简写大括号、同函数if-else分支深度过多等）
2. 重新设计数据的序列化和反序列化
   1. 提供json和bin两种格式数据接入，并提供两种格式的转换桥梁。开发状态可以使用json，生产使用bin
   2. 删除java的Serializable进行数据序列化的方法，工程实践上他从来不是稳定可靠的方式
      1. 当遇到字节码降级的时候，他会反序列化失败（如android 语法脱糖过程）
      2. 可能由于jvm实现具体细节导致不稳定
      3. 效率和性能并不是最优
      4. lib库混淆时，他会反序列化失败
   3. 使用Leb128编码：这在原作者的二进制数据格式上带来了5个百分点的优化（原作者：40.46%,现方案：35.67%）
   4. 二进制反序列化提速：数据初始化只需要6.5秒（原作者：45秒）
      1. 分别耗时在数据加载到内存，反序列化， 我们手写序列化和反序列化精确控制数据格式和组织方式（原始数据加载大约500毫秒即可完成，文件大小61M）
      2. 数据结构计算：我把计算结果缓存到本地文件系统。数据计算时间：25s降低到5s,缓存文件大小：178M

## 离线数据编辑和修改
``` 
   // 把数据dump到json中，根据自己的需要编辑这些文件
    new JSONLoader().dump(new File("src/main/resources/json/"), geoData);
    
    //编辑文件完成后，在程序运行前，使用jsonloader替换binloader，用于加载json格式的离线数据
    ILoader.Storage.setLoader(new JSONLoader());
    
    // 代码测试没问题，使用BinLoader再把数据dump为二进制格式
    new BinLoader().dump(new File("src/main/resources/data.bin"), geoData);
    
    //线上，取消JSONLoader
```


## 原作者readme

经纬度转省市区县乡镇，速度快 -- 单线程50000次/秒；精度高 -- 准确率99.9999%

还包含如下功能：

1：查询某个经纬度附近所有商圈，按距离排序

2：给定城市，输出城市级别

3：输入任何地区的全称/简称/code，输出该地区的全部信息

4：获取所有行政区划关系数据等

使用方法： import com.dengxq.lnglat2Geo.GeoTrans 里面的所有方法均为公有接口

接口文档，参考博客： https://blog.csdn.net/deng0515001/article/details/99606156

jar包依赖：https://mvnrepository.com/artifact/com.github.deng0515001/lnglat2Geo

有问题直接联系：QQ：451408963@qq.com

