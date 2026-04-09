package vip.mate.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.dashboard.model.CronJobRunEntity;
import vip.mate.dashboard.repository.CronJobRunMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CronJob 执行历史服务
 *
 * @author MateClaw Team
 */
@Service
@RequiredArgsConstructor
public class CronJobRunService {

    private final CronJobRunMapper runMapper;

    /**
     * 记录一次执行开始
     */
    public CronJobRunEntity recordStart(Long cronJobId, String triggerType, String conversationId) {
        CronJobRunEntity run = new CronJobRunEntity();
        run.setCronJobId(cronJobId);
        run.setConversationId(conversationId);
        run.setStatus("running");
        run.setTriggerType(triggerType);
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);
        return run;
    }

    /**
     * 记录执行完成
     */
    public void recordComplete(Long runId, Integer tokenUsage) {
        CronJobRunEntity run = runMapper.selectById(runId);
        if (run != null) {
            run.setStatus("completed");
            run.setFinishedAt(LocalDateTime.now());
            run.setTokenUsage(tokenUsage);
            runMapper.updateById(run);
        }
    }

    /**
     * 记录执行失败
     */
    public void recordFailed(Long runId, String errorMessage) {
        CronJobRunEntity run = runMapper.selectById(runId);
        if (run != null) {
            run.setStatus("failed");
            run.setFinishedAt(LocalDateTime.now());
            run.setErrorMessage(errorMessage);
            runMapper.updateById(run);
        }
    }

    /**
     * 查询某个 CronJob 的执行历史
     */
    public List<CronJobRunEntity> listByJobId(Long cronJobId, int limit) {
        return runMapper.selectList(
                new LambdaQueryWrapper<CronJobRunEntity>()
                        .eq(CronJobRunEntity::getCronJobId, cronJobId)
                        .orderByDesc(CronJobRunEntity::getStartedAt)
                        .last("LIMIT " + limit));
    }

    /**
     * 查询最近的执行记录
     */
    public List<CronJobRunEntity> listRecent(int limit) {
        return runMapper.selectList(
                new LambdaQueryWrapper<CronJobRunEntity>()
                        .orderByDesc(CronJobRunEntity::getStartedAt)
                        .last("LIMIT " + limit));
    }
}
