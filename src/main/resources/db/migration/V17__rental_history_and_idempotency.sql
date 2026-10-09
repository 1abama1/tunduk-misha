CREATE TABLE rental_document_tools (
    document_id bigint NOT NULL REFERENCES rental_documents(id),
    tool_id bigint NOT NULL REFERENCES tool_instances(id),
    PRIMARY KEY (document_id, tool_id)
);
INSERT INTO rental_document_tools SELECT contract_id, id FROM tool_instances WHERE contract_id IS NOT NULL ON CONFLICT DO NOTHING;
INSERT INTO rental_document_tools SELECT d.id, d.tool_id FROM rental_documents d JOIN tool_instances t ON t.id=d.tool_id ON CONFLICT DO NOTHING;
-- Fail rather than silently merge pre-existing duplicate offline IDs.
CREATE UNIQUE INDEX uq_rental_offline_id ON rental_documents(offline_id) WHERE offline_id IS NOT NULL;
CREATE INDEX idx_document_tools_tool ON rental_document_tools(tool_id);

CREATE TABLE operation_receipts (
 actor_id varchar(100) NOT NULL, operation_id varchar(100) NOT NULL,
 request_hash varchar(64) NOT NULL, response_json text NOT NULL,
 PRIMARY KEY(actor_id, operation_id)
);
