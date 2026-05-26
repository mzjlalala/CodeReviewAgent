package cn.com.pcauto.llm.service;

public class demo {
    public static void main(String[] args) {
        User user = getUserById(1001);
        
        String username = user.getUsername();
        
        System.out.println("用户名称：" + username);
    }

    // 模拟数据库查询方法
    private static User getUserById(int id) {
        return null;
    }
}

// 用户实体类
class User {
    private String username;
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
}