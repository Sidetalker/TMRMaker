import random
import re

concepts = {
    'suggestion': [
        "[I __think__ ]you __would__ __like__ %s.",
        "[I __think__ ]you should try %s.",
        "[I __think__ ]you should give %s a try.",
        "Why don't you __try__ %s?",
        "How about %s?",
        "What do you think of %s?",
        "How do you feel about %s?",
        "%s seems like it __would__ suit your needs.",
    ],

    'request_additional_info': [
        "Is there anything else[ __to_know__]?",
        "Are there any more details[ __to_know__]?",
        "Do you have any more __details__?",
        "Tell me more[ about __preferences__].",
        "Could you tell me a little bit more[ about __preferences__]?",
    ],

    'request_spending_preferences': [
        "How much __want__ to spend?",
    ],

    'request_time_preferences': [
        "When __want__ to __go__?",
    ],

    'request_distance_preferences': [
        "How far [away ]__want__ to travel?",
    ],

    'prior_experience_with_suggestion': [
        "Have you [ever ]been to %s[ __before__]?"
    ],

    'offer_help': [
        "How can I __help__[ you]?",
        "Is there anything I can do to __help__[ you]?",
        "What can I do for you?",
        "Can I help you find a place to __go__?",
    ],

    'give_up': [
        "You are too needy.",
        "I can't __find__ a place that meets __preferences__.",
        "Your needs are very specific! I give up!",
        "You should be less picky.",
        "There aren't any places to __go__ that I can __find__.",
    ],

    'importance_of_travel_time': [
        "Do you value __distance__ __greater__ than %s?",
        "Do you put a greater emphasis on __distance__ than %s?",
        "Do you care about __distance__ more than %s?",
        "Is __distance__ more important[ to you] than %s?",
        "Is %s less important[ to you] than __distance__?",
    ],

    'failure_to_answer': [
        "I don't think you answered my question.",
        "I said...",
        "Maybe you didn't hear me.",
        "I don't think you understood.",
        "I'm still wondering...",
        "I still want to know...",
        "Could you answer my question?",
        "It's a yes or no question...",
        "That's not a very clear answer.",
    ],
}

thesaurus = {
    '__to_know__': ["I should know about", "you can tell me", "you could provide"],
    '__want__': ["do you want", "do you expect", "are you looking", "are you hoping", "are you expecting", "are you willing"],
    '__details__': ["details", "information"],
    '__preferences__': ["your preferences", "what you like", "what you are looking for"],
    '__before__': ["before", "in the past"],
    '__think__': ["think", "feel that", "suspect", "suggest"],
    '__like__': ["like", "enjoy"],
    '__would__': ["would", "might"],
    '__try__': ["try", "go to", "eat at"],
    '__show__': ["show", "tell", "give"],
    '__go__': ["go", "eat", "dine"],
    '__travel__': ["travel", "drive", "go"],
    '__help__': ["help", "assist"],
    '__find__': ["find", "think of", "come up with"],
    '__distance__': ["distance", "travel time"],
    '__greater__': ["greater", "more"],
}

def respond_based_on_tmr(tmr):
    field = tmr.significant_field
    if field.type == restaurant:
        return convey(concepts['offer_suggestion'], field.value)

def convey(concept, *args):
    template = concepts[concept][random.randint(0, len(concepts[concept])-1)]
    if len(args):
        template = template % args

    for word, synonyms in thesaurus.iteritems():
        synonym = synonyms[random.randint(0, len(synonyms)-1)]
        template = template.replace(word, synonym)

    optionals_start = [m.start() for m in re.finditer('\[', template)]
    optionals_end = [m.start() for m in re.finditer('\]', template)]
    optionals = zip(optionals_start, optionals_end)

    template = list(template)
    
    removed = 0
    for optional in optionals:
        if random.randint(0, 1):
            template.pop(optional[0] - removed)
            removed += 1
            template.pop(optional[1] - removed)
            removed += 1
        else:
            template = template[:optional[0] - removed] \
                     + template[optional[1] + 1 - removed:]
            removed += optional[1] - optional[0] + 1

    template[0] = template[0].upper()

    return ''.join(template)
