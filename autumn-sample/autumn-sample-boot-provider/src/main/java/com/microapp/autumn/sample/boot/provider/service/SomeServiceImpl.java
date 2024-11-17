package com.microapp.autumn.sample.boot.provider.service;

import java.util.List;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microapp.autumn.api.annotation.Export;
import com.microapp.autumn.api.annotation.Reference;
import com.microapp.autumn.sample.api.SomeService;
import com.microapp.autumn.sample.api.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/10
 */
@Slf4j
@Export
@Service("someServiceImpl01")
public class SomeServiceImpl implements SomeService.Iface {

    @Reference
    SomeService.Iface someService;
    @Override
    public String echo(String msg) throws TException {
        return "msg";
    }

    @Override
    public int addUser(User user) throws TException {
        return 10;
    }

    @Override
    public List<User> findUserByIds(List<Integer> idList) throws TException {
        return List.of();
    }
}
