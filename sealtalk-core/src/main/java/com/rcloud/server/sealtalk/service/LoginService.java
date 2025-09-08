package com.rcloud.server.sealtalk.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rcloud.server.sealtalk.configuration.SealtalkConfig;
import com.rcloud.server.sealtalk.constant.Constants;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.dao.LoginTokenMapper;
import com.rcloud.server.sealtalk.entity.LoginToken;
import com.rcloud.server.sealtalk.exception.ParamException;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.AES256;
import com.rcloud.server.sealtalk.util.N3d;
import com.rcloud.server.sealtalk.util.RandomUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginService {


    @Autowired
    private LoginTokenMapper loginTokenMapper;

    @Autowired
    protected SealtalkConfig sealtalkConfig;


    public static Cache<Integer, LoginToken> TOKEN_CHCHE = CacheBuilder.newBuilder()
        .maximumSize(20000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    private LoginToken saveToken(Integer userId) {
        LoginToken insert = new LoginToken();
        insert.setUserId(userId);
        insert.setLoginTime(System.currentTimeMillis());
        loginTokenMapper.insert(insert);
        return insert;
    }

    private LoginToken getToken(Integer userId, Long id) {
        LoginToken result = TOKEN_CHCHE.getIfPresent(userId);
        if (result != null && id.equals(result.getId())) {
            return result;
        }
        result = loginTokenMapper.selectByPrimaryKey(id);
        if (result != null && userId.equals(result.getUserId())) {
            TOKEN_CHCHE.put(userId, result);
        }
        return result;
    }

    private void delToken(Long id){
        loginTokenMapper.deleteByPrimaryKey(id);
    }

    public void setCookie(HttpServletResponse response, int userId) {
        LoginToken loginToken = saveToken(userId);
        TOKEN_CHCHE.put(userId,loginToken);
        int salt = RandomUtil.randomBetween(1000, 9999);
        String text = salt + Constants.SEPARATOR_NO + userId + Constants.SEPARATOR_NO + System.currentTimeMillis() + Constants.SEPARATOR_NO + loginToken.getId();
        byte[] value = AES256.encrypt(text, sealtalkConfig.getAuthCookieKey());
        Cookie cookie = new Cookie(sealtalkConfig.getAuthCookieName(), new String(value));
        cookie.setHttpOnly(true);
        cookie.setDomain(sealtalkConfig.getAuthCookieDomain());
        cookie.setMaxAge(Integer.valueOf(sealtalkConfig.getAuthCookieMaxAge()));
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void removeCookie(Integer userId, Long tokenId, HttpServletResponse response) {
        //删除token
        TOKEN_CHCHE.invalidate(userId);
        delToken(tokenId);

        Cookie newCookie = new Cookie(sealtalkConfig.getAuthCookieName(), null);
        newCookie.setMaxAge(0);
        newCookie.setPath("/");
        newCookie.setDomain(sealtalkConfig.getAuthCookieDomain());
        response.addCookie(newCookie);
    }

    /**
     * @return [userId,userIdStr,tokenId]
     */
    public Triple<Integer,String,Long> getCurrentUserId(HttpServletRequest request) throws ParamException, ServiceException {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0){
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "cookie 不存在");
        }
        Cookie authCookie = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(sealtalkConfig.getAuthCookieName())) {
                authCookie = cookie;
                break;
            }
        }
        if (authCookie == null){
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "auth cookie 不存在");
        }
        String cookieValue = authCookie.getValue();
        String decrypt = AES256.decrypt(cookieValue.getBytes(), sealtalkConfig.getAuthCookieKey());
        assert decrypt != null;
        String[] split = decrypt.split(Constants.SEPARATOR_ESCAPE);
        if (split.length != 4) {
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "auth内容不对");
        }
        Integer userId = Integer.parseInt(split[1]);
        Long tokenId = Long.parseLong(split[3]);
        LoginToken token = getToken(userId, tokenId);
        if (token == null || !userId.equals(token.getUserId())){
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "token不对");
        }
        if ((System.currentTimeMillis() - token.getLoginTime()) > (7 * Constants.ONE_DAY_MILLION_SECONDS)){
            //过期后删除
            TOKEN_CHCHE.invalidate(userId);
            delToken(tokenId);
            throw new ParamException(ErrorCode.PARAM_ERROR.getErrorCode(), "token已过期");
        }
        String userIdStr = N3d.encode(userId);
        return Triple.of(userId,userIdStr,tokenId);
    }


}
