CREATE TABLE SIDE_EFFECT(
	id		                character varying(64),
	clientReferenceId		character varying(64) NOT NULL,
	tenantId		        character varying(1000),
    taskId		            character varying(64),
    taskClientReferenceId	character varying(64) NOT NULL,
    symptoms                jsonb,
	createdBy		        character varying(64),
    createdTime             bigint,
	lastModifiedBy		    character varying(64),
    lastModifiedTime        bigint,
	clientCreatedTime       bigint,
    clientLastModifiedTime  bigint,
	rowVersion              bigint,
	isDeleted               bool,
	CONSTRAINT uk_side_effect PRIMARY KEY (id),
	CONSTRAINT uk_side_effect_clientReference_id unique (clientReferenceId)
);