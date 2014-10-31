package com.box.sdk;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class BoxComment extends BoxResource {
    private static final Pattern MENTION_REGEX = Pattern.compile("@\\[.+:.+\\]");

    public BoxComment(BoxAPIConnection api, String id) {
        super(api, id);
    }

    static boolean messageContainsMention(String message) {
        return MENTION_REGEX.matcher(message).find();
    }

    public class Info extends BoxResource.Info {
        private boolean isReplyComment;
        private String message;
        private String taggedMessage;
        private BoxUser.Info createdBy;
        private Date createdAt;
        private BoxResource.Info item;
        private BoxUser.Info modifiedBy;

        /**
         * Constructs an empty Info object.
         */
        public Info() {
            super();
        }

        /**
         * Constructs an Info object by parsing information from a JSON string.
         * @param  json the JSON string to parse.
         */
        public Info(String json) {
            super(json);
        }

        /**
         * Constructs an Info object using an already parsed JSON object.
         * @param  jsonObject the parsed JSON object.
         */
        Info(JsonObject jsonObject) {
            super(jsonObject);
        }

        public String getMessage() {
            if (this.taggedMessage != null) {
                return this.taggedMessage;
            }

            return this.message;
        }

        public void setMessage(String message) {
            if (messageContainsMention(message)) {
                this.taggedMessage = message;
                this.addPendingChange("tagged_message", message);
                this.removePendingChange("message");
            } else {
                this.message = message;
                this.addPendingChange("message", message);
                this.removePendingChange("tagged_message");
            }
        }

        @Override
        public BoxComment getResource() {
            return BoxComment.this;
        }

        @Override
        protected void parseJSONMember(JsonObject.Member member) {
            super.parseJSONMember(member);

            try {
                String memberName = member.getName();
                JsonValue value = member.getValue();
                switch (memberName) {
                    case "is_reply_comment":
                        this.isReplyComment = value.asBoolean();
                        break;
                    case "message":
                        this.message = value.asString();
                        break;
                    case "tagged_message":
                        this.taggedMessage = value.asString();
                        break;
                    case "created_by":
                        JsonObject userJSON = value.asObject();
                        if (this.createdBy == null) {
                            String userID = userJSON.get("id").asString();
                            BoxUser user = new BoxUser(getAPI(), userID);
                            this.createdBy = user.new Info(userJSON);
                        } else {
                            this.createdBy.update(userJSON);
                        }
                        break;
                    case "created_at":
                        this.createdAt = BoxDateParser.parse(value.asString());
                        break;
                    case "item":
                        this.parseItem(value);
                        break;
                    case "modified_by":
                        userJSON = value.asObject();
                        if (this.modifiedBy == null) {
                            String userID = userJSON.get("id").asString();
                            BoxUser user = new BoxUser(getAPI(), userID);
                            this.modifiedBy = user.new Info(userJSON);
                        } else {
                            this.modifiedBy.update(userJSON);
                        }
                        break;
                    default:
                        break;
                }
            } catch (ParseException e) {
                assert false : "A ParseException indicates a bug in the SDK.";
            }
        }

        private void parseItem(JsonValue value) {
            JsonObject itemJSON = value.asObject();
            String itemType = itemJSON.get("type").asString();
            if (itemType.equals("file")) {
                this.updateItemAsFile(itemJSON);
            } else if (itemType.equals("comment")) {
                this.updateItemAsComment(itemJSON);
            }
        }

        private void updateItemAsFile(JsonObject itemJSON) {
            String itemID = itemJSON.get("id").asString();
            if (this.item != null && this.item instanceof BoxFile.Info && this.item.getID().equals(itemID)) {
                this.item.update(itemJSON);
            } else {
                BoxFile file = new BoxFile(getAPI(), itemID);
                this.item = file.new Info(itemJSON);
            }
        }

        private void updateItemAsComment(JsonObject itemJSON) {
            String itemType = itemJSON.get("type").asString();
            String itemID = itemJSON.get("id").asString();
            if (this.item != null && this.item instanceof BoxComment.Info && this.item.getID().equals(itemID)) {
                this.item.update(itemJSON);
            } else {
                BoxComment comment = new BoxComment(getAPI(), itemID);
                this.item = comment.new Info(itemJSON);
            }
        }
    }
}
