package com.dtstack.engine.api.vo;


import com.dtstack.engine.api.domain.BaseEntity;
import com.dtstack.engine.api.dto.AccountDTO;

import java.util.Objects;

/**
 * @author yuebai
 * @date 2020-02-14
 */
public class AccountVo  extends BaseEntity {

    private Long bindTenantId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 数栈用户名
     */
    private String username;

    /**
     * 数据库用户名
     */
    private String name;

    private String phone;

    private String email;

    private String modifyUserName;

    public String getModifyUserName() {
        return modifyUserName;
    }

    public void setModifyUserName(String modifyUserName) {
        this.modifyUserName = modifyUserName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String password;

    /**
     *  绑定用户ID
     */
    private Long bindUserId;

    public Long getBindUserId() {
        return bindUserId;
    }

    public void setBindUserId(Long bindUserId) {
        this.bindUserId = bindUserId;
    }

    public Long getBindTenantId() {
        return bindTenantId;
    }

    public void setBindTenantId(Long bindTenantId) {
        this.bindTenantId = bindTenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AccountVo() {
    }

    public AccountVo(AccountDTO accountDTO) {
        if(Objects.nonNull(accountDTO)){
            this.setName(accountDTO.getName());
            this.setUsername(accountDTO.getUsername());
            this.setModifyUserName(accountDTO.getModifyUserName());
            this.setGmtCreate(accountDTO.getGmtCreate());
            this.setGmtModified(accountDTO.getGmtModified());
            this.setId(accountDTO.getId());
        }
    }
}