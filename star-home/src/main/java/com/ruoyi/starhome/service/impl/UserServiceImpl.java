package com.ruoyi.starhome.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.starhome.domain.dto.UpdateUserRequest;
import com.ruoyi.starhome.service.IUserService;
import com.ruoyi.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public Integer updateUser(UpdateUserRequest request) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(request.getId());
        sysUser.setPhonenumber(request.getPhone());
        sysUser.setEmail(request.getEmail());
        sysUser.setStatus(String.valueOf(request.getStatus()));
        if(StringUtils.isNotBlank(request.getPassword())){
            sysUser.setPassword(SecurityUtils.encryptPassword(request.getPassword()));
        }
        return sysUserMapper.updateUser(sysUser);
    }
}
