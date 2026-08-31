package com.hmeclazcke.filegenerator.adapter.out.gemini;

import com.hmeclazcke.filegenerator.application.port.out.TextSeedProviderPort;
import org.springframework.ai.chat.model.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpringAiGeminiUniqueBakingTextSeedProvider implements TextSeedProviderPort {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SpringAiGeminiUniqueBakingTextSeedProvider.class);

    private static final int MAX_REFILL_ATTEMPTS = 3;
    private static final int LINES_PER_BATCH = 500;
    private static final String COULD_NOT_GENERATE_UNIQUE_LINES =
            "Gemini did not return new unique baking dataset lines";

    private static final List<String> TOPIC_GROUPS = List.of(
            "sourdough starters, flour strength, hydration, autolyse, bulk fermentation and proofing",
            "country loaves, rye bread, whole wheat bread, focaccia, baguettes and enriched sandwich bread",
            "croissants, puff pastry, laminated dough, butter plasticity, chilling and layer definition",
            "brioche, panettone, babka, enriched dough, sugar, eggs, butter and gluten development",
            "cakes, cookies, tarts, custards, meringues, sponge structure and ingredient ratios",
            "baking troubleshooting, dense crumb, gummy crumb, weak oven spring, scoring and steam",
            "bakery production planning, mixing schedules, dough temperature, scaling and batch consistency"
    );

    private static final List<String> LINE_STYLES = List.of(
            "practical recipe note",
            "ingredient explanation",
            "technique explanation",
            "troubleshooting observation",
            "process comparison",
            "temperature and timing note",
            "quality-control note"
    );

    private final ChatModel chatModel;
    private final Deque<String> bufferedLines = new ArrayDeque<>();
    private final Set<String> emittedLines = new HashSet<>();
    private long batchNumber = 1;

    public SpringAiGeminiUniqueBakingTextSeedProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String nextLine() {
        if (bufferedLines.isEmpty()) {
            refillBuffer();
        }

        // The filesystem adapter writes a line-delimited UTF-8 dataset.
        return bufferedLines.removeFirst() + "\n";
    }

    private void refillBuffer() {
        int attempts = 0;

        while (bufferedLines.isEmpty() && attempts < MAX_REFILL_ATTEMPTS) {
            attempts++;

            String generatedText = chatModel.call(buildPrompt(batchNumber));
            batchNumber++;

            List<String> uniqueLines = parseUniqueLines(generatedText);
            uniqueLines.forEach(bufferedLines::addLast);

            LOGGER.info(
                    "Generated Gemini baking dataset batch {} with {} new unique lines and {} total unique lines",
                    batchNumber - 1,
                    uniqueLines.size(),
                    emittedLines.size()
            );
        }

        if (bufferedLines.isEmpty()) {
            throw new IllegalStateException(COULD_NOT_GENERATE_UNIQUE_LINES);
        }
    }

    private String buildPrompt(long currentBatchNumber) {
        String topicGroup = TOPIC_GROUPS.get((int) ((currentBatchNumber - 1) % TOPIC_GROUPS.size()));
        String lineStyle = LINE_STYLES.get((int) ((currentBatchNumber - 1) % LINE_STYLES.size()));

        return """
                Generate unique source text lines for a baking and pastry RAG dataset.

                Batch:
                %d

                Main topic group:
                %s

                Line style:
                %s

                Rules:
                - Return only plain text.
                - Return one detailed standalone sentence per line.
                - Generate %d lines.
                - Do not use numbering.
                - Do not use markdown.
                - Do not include headings or explanations.
                - Do not repeat exact lines from earlier in this response.
                - Make each line specific enough to answer a future question.
                - Prefer concrete quantities, temperatures, times, textures, symptoms, causes and corrective actions.
                - Make each sentence at least 25 words long.
                - Mix sourdough bread, pastry, fermentation, flour, butter, temperature, timing and troubleshooting details.
                - Write in clear English.
                """.formatted(currentBatchNumber, topicGroup, lineStyle, LINES_PER_BATCH);
    }

    private List<String> parseUniqueLines(String generatedText) {
        return generatedText.lines()
                .map(this::cleanLine)
                .filter(line -> !line.isBlank())
                .filter(emittedLines::add)
                .toList();
    }

    private String cleanLine(String line) {
        return line.strip()
                .replaceFirst("^[-*]\\s+", "")
                .replaceFirst("^\\d+[.)]\\s+", "")
                .strip();
    }
}
