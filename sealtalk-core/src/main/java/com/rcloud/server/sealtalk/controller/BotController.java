package com.rcloud.server.sealtalk.controller;

import com.rcloud.server.sealtalk.model.dto.BotIntegrateDTO;

import com.rcloud.server.sealtalk.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rcloud.server.sealtalk.constant.BotTypeEnum;
import com.rcloud.server.sealtalk.constant.ErrorCode;
import com.rcloud.server.sealtalk.constant.LogType;
import com.rcloud.server.sealtalk.entity.BotInfo;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.service.BotService;
import com.rcloud.server.sealtalk.model.dto.BotDto;
import com.rcloud.server.sealtalk.model.dto.PhoneDTO;
import com.rcloud.server.sealtalk.model.response.APIResult;
import com.rcloud.server.sealtalk.model.response.APIResultWrap;
import com.rcloud.server.sealtalk.util.ValidateUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/botmanager")
@Slf4j
public class BotController extends BaseController {

    @Autowired
    private BotService botService;

    /**
     * 创建机器人
     */
    @PostMapping("/create")
    public APIResult<BotDto> createBot(@RequestBody BotDto botDto) throws Exception {

      // 参数校验
      botDto.check();
      log.info("{},{},{},{},{}", LogType.BOT_CREATE, botDto.getName(), botDto.getPortraitUri(), botDto.getOpeningMessage(), JacksonUtil.toJson(botDto.getIntegrations()));

      // 调用service创建机器人
      String botId = botService.createBot(botDto);
      botDto.setBotId(botId);

      if (botDto.getPhones() != null && botDto.getBotType() !=null && botDto.getBotType() == BotTypeEnum.PRIVATE.getType()) {
        botService.bindUserToBot(botDto.getPhones(), botId);
      }

      return APIResultWrap.ok(botDto);
    }

    /**
     * 批量查询机器人列表
     * @param limit 查询数量限制，默认10条
     * @return 机器人列表
     * @throws ServiceException 当查询失败时抛出
     */
    @GetMapping("/list")
    public APIResult<List<BotDto>> getBotList(@RequestParam(value = "limit", defaultValue = "50") Integer limit) throws ServiceException {
        
        log.info("批量查询机器人列表，limit：{}", limit);
        
        List<BotInfo> botList = botService.getBotList(limit);
        
        // 转换为BotDto列表
        List<BotDto> botDtoList = botList.stream()
                .map(botInfo -> {
                    BotDto botDto = new BotDto();
                    botDto.setBotId(botInfo.getBotId());
                    botDto.setName(botInfo.getName());
                    botDto.setPortraitUri(botInfo.getPortraitUri());
                    botDto.setOpeningMessage(botInfo.getOpeningMessage());
                    botDto.setBotType(botInfo.getBotType());
                    return botDto;
                })
                .collect(java.util.stream.Collectors.toList());
        
        return APIResultWrap.ok(botDtoList);
    }

    /**
     * 根据botId删除机器人
     * @param botId 机器人ID
     * @return 删除结果
     * @throws ServiceException 当参数校验失败时抛出
     */
    @DeleteMapping("/{botId}")
    public APIResult<Boolean> deleteBot(@PathVariable("botId") String botId) throws Exception {
        
        // 参数校验
        ValidateUtils.notBlank(botId,"botId");
        
        log.info("{},{}", LogType.BOT_DEL , botId);
        
        botService.deleteBot(botId);
        
        return APIResultWrap.ok();
    }

    /**
     * 绑定用户和机器人
     * @param botDto 机器人信息，包含botId(机器人ID)和phones(用户手机号列表)
     * @return 绑定结果
     * @throws ServiceException 当参数校验失败或绑定失败时抛出
     */
    @PostMapping("/bind")
    public APIResult<Boolean> bindUserToBot(@RequestBody BotDto botDto) throws Exception {
        
        // 参数校验
        ValidateUtils.notBlank(botDto.getBotId(),"botId");
        ValidateUtils.notNull(botDto.getPhones(),"phones");
        ValidateUtils.checkLength(botDto.getPhones(), 1, 20, "phones");
        // 验证手机号格式
        for (PhoneDTO phone : botDto.getPhones()) {
            ValidateUtils.checkNumberStr(phone.getRegion(), "region");
            ValidateUtils.checkNumberStr(phone.getPhone(), "phone");
        }
        
        // 查询机器人信息，检查机器人类型
        BotInfo botInfo = botService.getBotInfo(botDto.getBotId());
        if (botInfo == null) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), "bot id not found");
        }
        
        // 只有private类型的机器人才允许绑定
        if (botInfo.getBotType() == null || botInfo.getBotType() != BotTypeEnum.PRIVATE.getType()) {
            throw new ServiceException(ErrorCode.PARAM_ERROR.getErrorCode(), "only private bot type is supported");
        }
        
        // 调用service绑定用户和机器人
        botService.bindUserToBot(botDto.getPhones(), botDto.getBotId());
        
        return APIResultWrap.ok();
    }

    /**
     * 更新机器人信息
     * @param botDto 机器人信息，包含botId(机器人ID)和需要更新的字段
     * @return 更新结果
     * @throws ServiceException 当参数校验失败或更新失败时抛出
     */
    @PostMapping("/update")
    public APIResult<Boolean> updateBot(@RequestBody BotDto botDto) throws Exception {
        
        // 参数校验
        ValidateUtils.notBlank(botDto.getBotId(),"botId");

        if (botDto.getIntegrations() != null) {
          for (BotIntegrateDTO botIntegrateDTO: botDto.getIntegrations()){
              ValidateUtils.notBlank(botIntegrateDTO.getIntegrateType(), "integrate type");
          }
        }
        // 调用service更新机器人信息
        botService.updateBot(botDto);
        
        return APIResultWrap.ok();
    }

}
