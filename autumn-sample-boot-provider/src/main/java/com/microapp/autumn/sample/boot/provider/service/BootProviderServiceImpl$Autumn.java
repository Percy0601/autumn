package com.microapp.autumn.sample.boot.provider.service;

import org.springframework.stereotype.Service;

import com.microapp.autumn.sample.api.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/6
 */
@Slf4j
@Service
public class BootProviderServiceImpl$Autumn extends BootProviderServiceImpl{

    public java.lang.String echo(java.lang.String msg) throws org.apache.thrift.TException {
        return super.echo(msg);
    }

    public int addUser(User user) throws org.apache.thrift.TException {
        return 0;
    }

    public java.util.List<User> findUserByIds(java.util.List<java.lang.Integer> idList) throws org.apache.thrift.TException {
        return null;
    }
}