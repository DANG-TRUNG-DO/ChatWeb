CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id),
    content TEXT,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT' CHECK (type IN ('TEXT', 'IMAGE', 'FILE')),
    reply_to_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id, id DESC);
CREATE INDEX idx_messages_sender ON messages(sender_id);

-- Add foreign key for last_read_message_id after messages table exists
ALTER TABLE conversation_members
    ADD CONSTRAINT fk_last_read_message
    FOREIGN KEY (last_read_message_id) REFERENCES messages(id) ON DELETE SET NULL;
