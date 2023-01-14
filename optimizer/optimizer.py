import os

# constants
maps = ['maptestsmall', 'SmallElements', 'AllElements', 'DefaultMap']
constants_to_optimize = ['weight1', 'weight2']
bot_to_optimize = 'sphere'
optimized_name = 'optimized_sphere'


bot_template = ('constant 1', 'constant 2') # bots are represented as tuples of constants in the same order as the constants that are provided to optimize
resolution = (0.1, 0.1) # subjective opinion of smallest significant change
# run format:
# gradlew run -Pmaps=[map] -PteamA=[Team A] -PteamB=[Team B]

# mutate one stat by random * resolution
def mutate(bot):
    pass

# produce two offspring, one based on bot 1 with x crossover, and one based on bot2
def reproduce(bot1, bot2):
    pass

def run_match(a, b):
    wins = [0, 0]
    for map in maps:
        result = os.popen('gradlew run -Pmaps={map} -PteamA=oneeleven -PteamB=oneeleven'.format(map = map)).read().splitlines()
        print(result[-1])

def main():
    os.chdir('..')
    os.system('gradlew update')
    os.system('gradlew build')
    run_match(1, 2)

if __name__ == '__main__':
    main()