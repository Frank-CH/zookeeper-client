package com.jiuwei.commons.zkclient.core;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.jiuwei.commons.zkclient.config.ZkConfig;
import com.jiuwei.commons.zkclient.enums.WatchType;
import com.jiuwei.commons.zkclient.event.AbstractZKListener;
import com.jiuwei.commons.zkclient.event.ZKListener;
import com.jiuwei.commons.zkclient.exception.ZookeeperException;
import com.jiuwei.commons.zkclient.helper.ObjectHelper;
import com.jiuwei.commons.zkclient.helper.StringHelper;

/**
 *
 * @author cpthack cpt@jianzhimao.com
 * @date Aug 6, 2016 3:17:28 PM <br/>
 * @version
 * @since JDK 1.7
 */
public class CuratorZKClient extends AbstractZKListener implements ZKClient {

	private final static Logger logger = LoggerFactory
			.getLogger(CuratorZKClient.class);
	private final static CuratorFrameworkBuilder builder = new CuratorFrameworkBuilder();
	static CuratorFramework zkClient = null;
	private ZkConfig zkConfig = builder.getZkconfig();
	private final static boolean isWatch = false;
	
	
	public CuratorZKClient build() {
		return build(zkConfig);
	}

	public CuratorZKClient build(ZkConfig zkConfig) {
		this.zkConfig = zkConfig;
		builder.setZkconfig(zkConfig);
		zkClient = builder.createCurator();
		return new CuratorZKClient();
	}

	@Override
	public void create(String path, String data) {
		Preconditions.checkArgument(!StringHelper.isNullOrEmpty(path),
				"path can not be null or empty!");

		if (exists(path)) {
			throw new ZookeeperException("zookeeper集群命名空间["
					+ zkConfig.getNamespace() + "]已存在节点[" + path + "]");
		}

		try {
			zkClient.create()
					.creatingParentsIfNeeded()
					// 当父节点不存在时，自动创建
					.withMode(CreateMode.PERSISTENT)
					// 存储类型（临时的还是持久的）
					// .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)//访问权限
					.forPath(path,
							StringHelper.getBytes(data, zkConfig.getCharset()));
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("创建zookeeper节点[{" + path + "}]失败，原因："
					+ e.getMessage());
		}
	}

	@Override
	public void delete(String path) {
		if (!exists(path)) {
			throw new ZookeeperException("zookeeper集群命名空间["
					+ zkConfig.getNamespace() + "]不存在节点[" + path + "]");
		}

		try {
			zkClient.delete().deletingChildrenIfNeeded().forPath(path);
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("删除zookeeper节点[" + path + "]失败，原因："
					+ e.getMessage());
		}
	}

	@Override
	public void update(String path, String data) {
		if (!exists(path)) {
			throw new ZookeeperException("zookeeper集群命名空间["
					+ zkConfig.getNamespace() + "]不存在节点[" + path + "]");
		}

		try {
			zkClient.setData().forPath(path,
					StringHelper.getBytes(data, zkConfig.getCharset()));
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("修改zookeeper节点[" + path + "]的数据为["
					+ data + "]失败，原因：" + e.getMessage());
		}
	}

	@Override
	public List<String> getChildren(String path) {
		logger.debug("获取zookeeper节点[{}]下的子节点", path);
		if (!exists(path)) {
			throw new ZookeeperException("zookeeper集群命名空间["
					+ zkConfig.getNamespace() + "]不存在节点[" + path + "]");
		}

		List<String> childrenNodes = null;
		try {
			childrenNodes = zkClient.getChildren().forPath(path);
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("获取zookeeper节点[" + path
					+ "]的子节点失败，原因：" + e.getMessage());
		}
		return childrenNodes;
	}

	@Override
	public String getData(String path) {
		String data = "";
		logger.debug("获取zookeeper节点[{}]的数据", path);
		
		if(!getChildren(path).isEmpty()){
			logger.warn("["+path+"]是一個目录，无法获取具体节点数据.");
			return null;
		}
		
		try {
			data = StringHelper.newString(zkClient.getData().forPath(path),
					zkConfig.getCharset());
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("获取zookeeper节点[" + path + "]的数据失败，原因："
					+ e.getMessage());
		}
		return data;
	}

	@Override
	public boolean exists(String path) {
		Preconditions.checkArgument(!StringHelper.isNullOrEmpty(path),
				"path cant not be null or empty!");

		boolean exists = false;
		try {
			exists = ObjectHelper.isNotNull(zkClient.checkExists()
					.forPath(path));
		} catch (Exception e) {
			logger.error("", e);
			throw new ZookeeperException("判断zookeeper节点[" + path + "]失败，原因："
					+ e.getMessage());
		}
		return exists;
	}
	
	@Override
	public void pathChildrenWatch(String path, ZKListener listener) {
		 watch();
		setWatchPath(WatchType.PathChildWatch, path, listener);
	}

	@Override
	public void nodeWatch(String path, ZKListener listener) {
		watch();
		setWatchPath(WatchType.NodeWatch, path, listener);
	}

	@Override
	public void treeWatch(String path, ZKListener listener) {
		watch();
		setWatchPath(WatchType.TreeWatch, path, listener);
	}
	
	
	private synchronized void watch(){
		if (!isWatch) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						CountDownLatch countDownLatch = new CountDownLatch(1);
						rootTreeWatch();
						countDownLatch.await();
					} catch (InterruptedException e) {
						logger.error("启动根节点变更事件监听线程出错",e);
					}
				}
			});
			thread.start();
		}
	}

	private synchronized void rootTreeWatch() {
		try {
			// 所有子节点的监听
			TreeCacheListener treeCacheListener = new TreeCacheListener() {
				public void childEvent(CuratorFramework client,
						TreeCacheEvent event) throws Exception {
					// 接受所有子节点的变更事件，并发送事件到监听器做统一调度
					WatchPathMulticaster(event);
				}
			};
			// 监听根节点下所有变更事件
			TreeCache treeCache = new TreeCache(zkClient, "/");
			treeCache.start();
			treeCache.getListenable().addListener(treeCacheListener);

		} catch (Exception e) {
			logger.error("监听根节点下所有变更事件出错",e);
		}
	}

}
