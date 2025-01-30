package com.microapp.autumn.sample.boot.provider.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.api.User;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/6
 */
@Slf4j
@Service
public class BootProviderServiceImpl$Autumn {
    @Resource(name = "bootProviderServiceImpl")
    SomeService.Iface bootProviderServiceImpl;
    public java.lang.String echo(java.lang.String msg) throws org.apache.thrift.TException {
        return bootProviderServiceImpl.echo(msg);
    }

    public int addUser(User user) throws org.apache.thrift.TException {
        return 0;
    }

    public java.util.List<User> findUserByIds(java.util.List<java.lang.Integer> idList) throws org.apache.thrift.TException {
        return null;
    }
}
