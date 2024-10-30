package com.infolink.dfs.shared;

import java.util.Date;
import java.util.List;


public class DfsFile {
	private String hash; // Hash of the file content, useful for deduplication
    private String owner;
    private String name; // Name of the file or directory
    private String path; // Full path for easier access, e.g., "/user/docs/example.txt"
    private boolean isDirectory; // To differentiate between files and directories
    private String parentHash; // Reference to the parent node's ID, null if it's the root
    private long size;
    private Date createTime;   
    private Date lastModifiedTime;
    
    public DfsFile() {
    	this.createTime = new Date();
    	this.lastModifiedTime = new Date();
    }
    
    public DfsFile(String hash, String owner, String name, String path, long size, boolean isDirectory, String parentHash, List<String> blockHashes) {
        this.hash = hash;
        this.owner = owner;
        this.name = name;
        this.path = path;
        this.size = size;
        this.isDirectory = isDirectory;
        this.parentHash = parentHash;
        this.blockHashes = blockHashes;
    	this.createTime = new Date();
    	this.lastModifiedTime = new Date();
    }
    
    // For files, contains content hash and block hashes
    private List<String> blockHashes; // For chunked storage if needed
    
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public boolean isDirectory() {
		return isDirectory;
	}
	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
	public String getParentHash() {
		return parentHash;
	}
	public void setParentHash(String parentHash) {
		this.parentHash = parentHash;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public List<String> getBlockHashes() {
		return blockHashes;
	}
	public void setBlockHashes(List<String> blockHashes) {
		this.blockHashes = blockHashes;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}
	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}

}

