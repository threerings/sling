//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.InputClickCallback;
import com.threerings.sling.gwt.client.SlingNav.FAQ;
import com.threerings.sling.gwt.ui.NamedRowSmartTable;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.web.data.Category;
import com.threerings.sling.web.data.Question;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Static methods for creation of FAQ section panels.
 */
public class FAQPanels extends FlowPanel
{
    /**
     * Creates a panel to view the given FAQs.
     */
    public static FlowPanel view (SlingContext ctx, List<Category> categories)
    {
        FlowPanel panel = Widgets.newFlowPanel("uFaqPanel");
        if (ctx.isSupport()) {
            panel.add(new ParaPanel(SlingUtils.makeLink(ctx, _msgs.edit(), FAQ.edit()), "EditLink"));
        }
        panel.add(new ParaPanel(_msgs.frequentlyAskedQuestions(), "Title"));

        for (Category cat : categories) {
            panel.add(new ParaPanel(cat.name, "Category"));
            for (Question question : cat.questions) {
                final Question fquestion = question;
                panel.add(new ParaPanel(Widgets.newActionLabel(question.question,
                    new ClickHandler() {
                        @Override
                        public void onClick (ClickEvent event)
                        {
                            Popups.info(fquestion.answer);
                        }
                    }), "Question"));
            }
        }
        return panel;
    }

    /**
     * Creates a panel to edit the given FAQs.
     */
    public static FlowPanel edit (SlingContext ctx, List<Category> categories)
    {
        FlowPanel panel = Widgets.newFlowPanel("uFaqPanel",
            new ParaPanel(SlingUtils.makeLink(ctx, _msgs.view(), FAQ.view()), "ViewLink"),
            new ParaPanel(_msgs.editFaqs(), "Title"),
            new ParaPanel(Widgets.newFlowPanel(
                Widgets.newHTML(_msgs.faqEditPart1()), SlingUtils.makeLink(
                    ctx, _msgs.faqEditAddQuestion(), FAQ.editQuestion(0))), "Instructions"));

        for (Category cat : categories) {
            panel.add(new ParaPanel(cat.name, "Category"));
            for (Question question : cat.questions) {
                panel.add(new ParaPanel(SlingUtils.makeLink(ctx, question.question,
                    FAQ.editQuestion(question.questionId)), "Question"));
            }
        }

        return panel;
    }

    /**
     * Creates a panel to edit the question with the given id from the given FAQs. If a question
     * of the given id is not found, the panel is configured to add a new question.
     */
    public static Widget editQuestion (final SlingContext ctx, List<Category> categories,
        int questionId)
    {
        final NamedRowSmartTable table = new NamedRowSmartTable("uFaqPanel", 0, 5);

        final Question question = findOrCreateQuestion(categories, questionId);
        final ItemListBox<Category> categoryList = new ItemListBox<Category>() {
            @Override
            protected String toLabel (Category cat)
            {
                return cat.name;
            }
        };

        for (Category cat : categories) {
            categoryList.addItem(cat);
            if (question.categoryId == cat.categoryId) {
                categoryList.setSelectedItem(cat);
            }
        }

        int row = 0;
        table.cell(row, 0).text(
            question.questionId == 0 ? _msgs.addNewFaqQuestion() : _msgs.editFaqQuestion())
            .colSpan(2).styles("Title");
        row++;

        Button addCategoryBtn = new Button(_msgs.addNew());
        table.cell(row, 0).text(_msgs.category()).styles("Label");
        table.cell(row, 1).widget(Widgets.newRow(categoryList, addCategoryBtn));
        row++;

        final TextBox questionText = Widgets.newTextBox(question.question, 230, 50);
        table.cell(row, 0).text(_msgs.question()).styles("Label");
        table.cell(row, 1).widget(questionText);
        row++;

        final TextArea answerText = Widgets.newTextArea(question.answer, 50, 8);
        table.cell(row, 0).text(_msgs.answer()).styles("Label", "AnswerLabel");
        table.cell(row, 1).widget(answerText);
        row++;

        Button storeQuestionBtn = new Button(_msgs.submit());
        table.cell(row, 0).text("");
        table.cell(row, 1).widget(storeQuestionBtn);
        table.nameRow("submit", row++);

        new InputClickCallback<Integer, TextBox>(addCategoryBtn, Widgets.newTextBox("", 230, 50)) {
            @Override
            protected boolean callService (String input)
            {
                _category = new Category();
                _category.name = input;
                _category.questions = Lists.newArrayList();
                ctx.svc.storeCategory(_category, this);
                return true;
            }

            @Override
            protected boolean gotResult (Integer result)
            {
                _category.categoryId = result;
                categoryList.addItem(_category);
                categoryList.setSelectedItem(_category);
                return true;
            }

            @Override
            protected String formatError (Throwable cause)
            {
                return translateServerError(cause);
            }

            Category _category;
        }.setPromptText(_msgs.enterCategoryName())
         .setConfirmChoices(_msgs.createCategory(), _msgs.cancel());

        new ClickCallback<Integer>(storeQuestionBtn) {
            @Override
            protected boolean callService ()
            {
                Category cat = categoryList.getSelectedItem();
                if (cat == null) {
                    Popups.error(_msgs.selectOrCreateNewCategory());
                    return false;
                }
                question.categoryId = cat.categoryId;
                if ((question.question = questionText.getText()).length() == 0) {
                    Popups.error(_msgs.enterQuestion());
                    return false;
                }
                if ((question.answer = answerText.getText()).length() == 0) {
                    Popups.error(_msgs.enterAnswer());
                    return false;
                }
                ctx.svc.storeQuestion(question, this);
                return true;
            }

            @Override
            protected boolean gotResult (Integer result)
            {
                table.cell("submit", 1).widget(
                    Widgets.newRow(Widgets.newLabel(_msgs.questionSubmitted()), SlingUtils.makeLink(
                        ctx, _msgs.returnToEditFaqs(), FAQ.edit()), SlingUtils.makeLink(ctx,
                        _msgs.returnToView(), FAQ.view())));
                return false;
            }

            @Override
            protected String formatError (Throwable cause)
            {
                return translateServerError(cause);
            }
        };
        return table;
    }

    protected static Question findOrCreateQuestion (List<Category> categories, int questionId)
    {
        for (Category cat : categories) {
            for (Question q : cat.questions) {
                if (q.questionId == questionId) {
                    return q;
                }
            }
        }
        Question q = new Question();
        q.categoryId = categories.isEmpty() ? 0 : categories.get(0).categoryId;
        return q;
    }

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
