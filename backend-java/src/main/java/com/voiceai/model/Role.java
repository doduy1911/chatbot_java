package com.voiceai.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @Column(name = "roleid")
    private Integer roleid;

    @Column(name = "rolename", nullable = false)
    private String rolename;

    public Integer getRoleid() { return roleid; }
    public void setRoleid(Integer roleid) { this.roleid = roleid; }

    public String getRolename() { return rolename; }
    public void setRolename(String rolename) { this.rolename = rolename; }
}
