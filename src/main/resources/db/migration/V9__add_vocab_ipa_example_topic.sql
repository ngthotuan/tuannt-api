-- Pronunciation (IPA), an example sentence and an optional topic for grouping practice sets.
-- One ALTER per column: older TiDB versions reject multi-column schema changes.
ALTER TABLE vocab_word ADD COLUMN ipa VARCHAR(100) NULL;
ALTER TABLE vocab_word ADD COLUMN example VARCHAR(500) NULL;
ALTER TABLE vocab_word ADD COLUMN topic VARCHAR(60) NULL;
CREATE INDEX idx_vocab_word_topic ON vocab_word (topic);
