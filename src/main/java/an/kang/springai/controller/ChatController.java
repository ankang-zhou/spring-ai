package an.kang.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController("/spring")
public class ChatController {

    private final ChatClient chatClient;

    // Boot 已自动配置好 Builder，直接构造器注入
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 聊天
     * @param message 消息
     * @return 回复
     */
    @GetMapping("/ai/chat")
    public String chat(@RequestParam String message) {
        // 开启一次新的对话请求构建，返回一个Prompt构建器对象，用来统一管理后续所有请求参数。
        return chatClient.prompt()
                // 往请求里添加用户侧的提示词内容，自动封装成符合大模型接口规范的User Message对象。
                .user(message)
                // 触发同步阻塞调用，当前线程会一直等待大模型生成完完整的全部响应内容，才会继续往下执行。
                .call()
                // 从返回的完整响应对象里，直接提取AI生成的纯文本内容，自动忽略token用量、结束原因等元数据。
                .content();
    }

    /**
     * 流式聊天
     * @param message 消息
     * @return 回复
     */
    @GetMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    @GetMapping("/ai/system/chat")
    public String systemChat(@RequestParam(required = false, defaultValue = "合同违约金上限如何计算？") String question) {
        // 1. 准备媒体内容 (注意：实际生产中建议从 OSS 或本地文件路径读取，而非 ClassPath)
        ClassPathResource resource = new ClassPathResource("/clause.png");

        // 2. 构建 Media 对象
        Media clauseMedia = Media.builder()
                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new ClassPathResource("/clause.png"))
                .build();

        // 3. 执行调用
        return chatClient.prompt()
                .system(s -> s.text("你是一名严谨的{role}，回答必须给出依据。")
                        .param("role", "法律顾问"))
                .user(u -> u.text("请解释：{question}")
                        .param("question", question)
                        .media(clauseMedia))
                .call()
                .content();
    }
}