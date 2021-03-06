# zookeeper-client
基于Curator ZK客户端连接工具进行简易封装，并基于事件驱动模型增强节点监听功能，使节点监听操作更加建议方便

## 修改ZK配置文件
https://github.com/cpthack/zookeeper-client/blob/master/src/main/resources/zk-config.properties
		
		#zookeeper客户端配置文件
		#zookeeper连接地址
		zk.config.address=127.0.0.1:2181,127.0.0.1:2181

		#zookeeper编码
		zk.config.charset=utf-8

		#zookeeper连接超时时间
		zk.config.connectionTimeoutMs=15_000

		#zookeeper的session超时时间
		zk.config.sessionTimeoutMs=60_000

		#zookeeper命名空间
		zk.config.namespace=
		/*
		 * 监听节点 example
		 */
		zKClient.nodeWatch("/test/abc", new ZKListener() {
	
## 使用示例：
		
		ZKClient zKClient = ZKClientBuilder.build();
		for(int i=0;i<100000;i++)
			zKClient = ZKClientBuilder.build();
		
		/*
		 * 监听节点 example
		 */
		zKClient.nodeWatch("/test/abc", new ZKListener() {
			
			@Override
			public void watchNode(ZKEventData eventData) {
				System.out.println("我监听的是【watchNode】，路径【/test】。"
						+ eventData.getEventType().getTypeDesc() + ", "
						+ eventData.getPath() + " , " + eventData.getData());
			}
		});
		zKClient.pathChildrenWatch("/test/abc", new ZKListener() {
			@Override
			public void watchNode(ZKEventData eventData) {
				System.out.println("我监听的是【pathChildrenWatch】，路径【/test】。"
						+ eventData.getEventType().getTypeDesc() + ", "
						+ eventData.getPath() + " , " + eventData.getData());
			}
		});
		zKClient.treeWatch("/test/abc", new ZKListener() {

			@Override
			public void watchNode(ZKEventData eventData) {
				System.out.println("我监听的是【treeWatch】，路径【/test】。"
						+ eventData.getEventType().getTypeDesc() + ", "
						+ eventData.getPath() + " , " + eventData.getData());
			}
		});
		
		/*
		 * 创建节点 example
		 */
		zKClient.create("/test/abc/t", "我叫成佩涛");
		
		
		/*
		 * 修改节点数据 example
		 */
		zKClient.update("/test/abc/t", "my name is cpthack.");
		
		
		/*
		 * 刪除节点 example
		 */
		zKClient.delete("/test/abc/t");
		
		
		/*
		 * 遍历path下一级子节点并读取每个节点的数据
		 */
		String rootPath="/";
		List<String> pathList = zKClient.getChildren(rootPath);
		System.out.println("开始获取ZK中PATH=[/]下的一级子节点及数据");
		for(String path:pathList){
			System.out.println("The path is ["+rootPath+path+"],The Data is ["+zKClient.getData(rootPath+path)+"].");
		}

## 具体可参考测试类：
https://github.com/cpthack/zookeeper-client/blob/master/src/test/java/com/jiuwei/commons/zkclient/ZKClientTest.java
